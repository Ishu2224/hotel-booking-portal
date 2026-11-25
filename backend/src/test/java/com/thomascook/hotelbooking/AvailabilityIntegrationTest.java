package com.thomascook.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thomascook.hotelbooking.dto.request.BookingLookupRequest;
import com.thomascook.hotelbooking.dto.request.CreateBookingRequest;
import com.thomascook.hotelbooking.dto.response.BookingResponse;
import com.thomascook.hotelbooking.entity.Booking;
import com.thomascook.hotelbooking.entity.BookingStatus;
import com.thomascook.hotelbooking.entity.Hotel;
import com.thomascook.hotelbooking.exception.NoAvailabilityException;
import com.thomascook.hotelbooking.repository.HotelRepository;
import com.thomascook.hotelbooking.service.AvailabilityService;
import com.thomascook.hotelbooking.service.BookingService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class AvailabilityIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private AvailabilityService availabilityService;
    @Autowired
    private BookingService bookingService;

    private final LocalDate base = TestDates.next(DayOfWeek.MONDAY);

    private Hotel hotel() {
        return hotelRepository.findById(THREE_ROOM_HOTEL_ID).orElseThrow();
    }

    private Booking insert(LocalDate in, LocalDate out, BookingStatus status) {
        int n = SEQ.incrementAndGet();
        return bookingRepository.save(Booking.builder()
                .bookingReference("TT-" + n).hotel(hotel()).guestName("Guest " + n).guestEmail("g" + n + "@example.com")
                .checkInDate(in).checkOutDate(out).nights((int) java.time.temporal.ChronoUnit.DAYS.between(in, out))
                .pricePerNight(new BigDecimal("12500.00")).weekendSurchargePercent(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("12500.00")).status(status).createdAt(LocalDateTime.now()).build());
    }

    private CreateBookingRequest request(int guest, LocalDate in, LocalDate out) {
        return new CreateBookingRequest(THREE_ROOM_HOTEL_ID, in, out, "Guest " + guest, "guest" + guest + "@example.com");
    }

    /**
     * An existing stay is [base+10, base+14). Half-open overlap: existing.in < req.out AND existing.out > req.in.
     * Offsets are days from the base date.
     */
    @ParameterizedTest(name = "request [{0},{1}) vs existing [10,14) -> overlaps={2}")
    @CsvSource({
            "14, 16, false",   // new guest arrives the day the previous one leaves (turnover day)
            "8,  10, false",   // new guest leaves the day the next one arrives
            "8,  11, true",    // overlaps the first night
            "13, 15, true",    // overlaps the last night
            "11, 13, true",    // fully inside
            "9,  15, true",    // fully contains
            "10, 14, true"     // identical
    })
    void overlapRuleIsHalfOpen(int reqIn, int reqOut, boolean overlaps) {
        insert(base.plusDays(10), base.plusDays(14), BookingStatus.CONFIRMED);

        int available = availabilityService.availableRooms(hotel(), base.plusDays(reqIn), base.plusDays(reqOut));

        assertThat(available).isEqualTo(overlaps ? 2 : 3);
    }

    @Test
    void cancelledBookingsDoNotConsumeRooms() {
        insert(base, base.plusDays(3), BookingStatus.CANCELLED);
        insert(base, base.plusDays(3), BookingStatus.CONFIRMED);

        assertThat(availabilityService.availableRooms(hotel(), base, base.plusDays(3))).isEqualTo(2);
    }

    @Test
    void availabilityNeverGoesNegative() {
        for (int i = 0; i < 5; i++) {
            insert(base, base.plusDays(2), BookingStatus.CONFIRMED); // more rows than rooms (e.g. bad data)
        }

        assertThat(availabilityService.availableRooms(hotel(), base, base.plusDays(2))).isZero();
    }

    @Test
    void backToBackStaysCanShareTheSameRoomOnTurnoverDay() {
        LocalDate checkout = base.plusDays(3);
        for (int i = 1; i <= 3; i++) {
            bookingService.createBooking(request(i, base, checkout)); // sells out [base, base+3)
        }

        // hotel is full for the nights themselves ...
        assertThatThrownBy(() -> bookingService.createBooking(request(4, base.plusDays(1), base.plusDays(2))))
                .isInstanceOf(NoAvailabilityException.class);
        // ... but a stay that starts on the check-out day is fine
        BookingResponse next = bookingService.createBooking(request(5, checkout, checkout.plusDays(2)));
        assertThat(next.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancellationReleasesInventoryForTheSameDates() {
        LocalDate out = base.plusDays(3);
        BookingResponse first = bookingService.createBooking(request(1, base, out));
        bookingService.createBooking(request(2, base, out));
        bookingService.createBooking(request(3, base, out));
        assertThat(availabilityService.availableRooms(hotel(), base, out)).isZero();
        assertThatThrownBy(() -> bookingService.createBooking(request(4, base, out)))
                .isInstanceOf(NoAvailabilityException.class);

        BookingResponse cancelled = bookingService.cancel(new BookingLookupRequest(first.bookingReference(), "guest1@example.com"));

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(availabilityService.availableRooms(hotel(), base, out)).isEqualTo(1);
        assertThat(bookingService.createBooking(request(4, base, out)).status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
