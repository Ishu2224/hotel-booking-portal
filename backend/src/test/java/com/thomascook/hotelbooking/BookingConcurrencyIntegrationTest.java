package com.thomascook.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.thomascook.hotelbooking.dto.request.CreateBookingRequest;
import com.thomascook.hotelbooking.dto.response.BookingResponse;
import com.thomascook.hotelbooking.entity.Booking;
import com.thomascook.hotelbooking.exception.NoAvailabilityException;
import com.thomascook.hotelbooking.service.BookingService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Fires simultaneous booking requests at a hotel with 3 rooms. The PESSIMISTIC_WRITE lock on the hotel row must
 * serialize them: exactly the free rooms are sold and every other request gets NoAvailabilityException (HTTP 409).
 * Any other exception (deadlock, lock timeout, constraint violation) propagates and fails the test.
 */
class BookingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    private final LocalDate checkIn = TestDates.next(DayOfWeek.MONDAY);
    private final LocalDate checkOut = checkIn.plusDays(2);

    private record Outcome(BookingResponse booking, boolean soldOut) {
    }

    private List<Outcome> raceForRooms(int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int guest = i;
                Callable<Outcome> task = () -> {
                    ready.countDown();
                    go.await();
                    try {
                        return new Outcome(bookingService.createBooking(new CreateBookingRequest(
                                THREE_ROOM_HOTEL_ID, checkIn, checkOut, "Racer " + guest, "racer" + guest + "@example.com")), false);
                    } catch (NoAvailabilityException e) {
                        return new Outcome(null, true);
                    }
                };
                futures.add(pool.submit(task));
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown(); // release every thread at the same instant
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void manySimultaneousRequestsNeverSellMoreRoomsThanExist() throws Exception {
        List<Outcome> outcomes = raceForRooms(8);

        List<Outcome> confirmed = outcomes.stream().filter(o -> !o.soldOut()).collect(Collectors.toList());
        assertThat(confirmed).hasSize(3);
        assertThat(outcomes.stream().filter(Outcome::soldOut)).hasSize(5);
        assertThat(bookingRepository.count()).isEqualTo(3);
        assertThat(confirmed.stream().map(o -> o.booking().bookingReference()).distinct()).hasSize(3);
    }

    @Test
    void twoGuestsRacingForTheLastRoomOnlyOneWins() throws Exception {
        for (int i = 0; i < 2; i++) { // leave exactly one room
            bookingService.createBooking(new CreateBookingRequest(
                    THREE_ROOM_HOTEL_ID, checkIn, checkOut, "Early " + i, "early" + i + "@example.com"));
        }

        List<Outcome> outcomes = raceForRooms(2);

        assertThat(outcomes.stream().filter(o -> !o.soldOut())).hasSize(1);
        assertThat(outcomes.stream().filter(Outcome::soldOut)).hasSize(1);
        List<Booking> all = bookingRepository.findAll();
        assertThat(all).hasSize(3);
    }
}
