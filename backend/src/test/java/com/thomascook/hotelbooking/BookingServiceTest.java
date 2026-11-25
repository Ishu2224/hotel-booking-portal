package com.thomascook.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thomascook.hotelbooking.config.PricingProperties;
import com.thomascook.hotelbooking.dto.request.BookingLookupRequest;
import com.thomascook.hotelbooking.dto.request.CreateBookingRequest;
import com.thomascook.hotelbooking.dto.response.BookingResponse;
import com.thomascook.hotelbooking.entity.Booking;
import com.thomascook.hotelbooking.entity.BookingStatus;
import com.thomascook.hotelbooking.entity.Hotel;
import com.thomascook.hotelbooking.exception.InvalidBookingStateException;
import com.thomascook.hotelbooking.exception.NoAvailabilityException;
import com.thomascook.hotelbooking.exception.ResourceNotFoundException;
import com.thomascook.hotelbooking.repository.BookingRepository;
import com.thomascook.hotelbooking.repository.HotelRepository;
import com.thomascook.hotelbooking.service.AvailabilityService;
import com.thomascook.hotelbooking.service.BookingReferenceGenerator;
import com.thomascook.hotelbooking.service.BookingService;
import com.thomascook.hotelbooking.service.PricingService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String REF = "TC-2026-ABC234";

    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingReferenceGenerator referenceGenerator;

    private BookingService service;
    private Hotel hotel;
    private final LocalDate checkIn = TestDates.next(DayOfWeek.MONDAY);
    private final LocalDate checkOut = checkIn.plusDays(3);

    @BeforeEach
    void setUp() {
        PricingService pricing = new PricingService(new PricingProperties(new BigDecimal("15"), "INR"));
        service = new BookingService(hotelRepository, bookingRepository, pricing,
                new AvailabilityService(bookingRepository), referenceGenerator);
        hotel = Hotel.builder().id(1L).name("Test Hotel").location("Mumbai").description("desc")
                .roomType("Deluxe").pricePerNight(new BigDecimal("5000.00")).totalRooms(2).maxGuestsPerRoom(2).build();
    }

    private CreateBookingRequest request() {
        return new CreateBookingRequest(1L, checkIn, checkOut, "  Asha Rao ", " Asha@Example.COM ");
    }

    private Booking existingBooking(BookingStatus status) {
        return Booking.builder().id(10L).bookingReference(REF).hotel(hotel).guestName("Asha Rao")
                .guestEmail("asha@example.com").checkInDate(checkIn).checkOutDate(checkOut).nights(3)
                .pricePerNight(new BigDecimal("5000.00")).weekendSurchargePercent(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("15000.00")).status(status).createdAt(LocalDateTime.now()).build();
    }

    // ---- create ----

    @Test
    void createsConfirmedBookingWithBackendCalculatedTotal() {
        when(hotelRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hotel));
        when(bookingRepository.countOverlappingConfirmed(1L, checkIn, checkOut)).thenReturn(1L); // 1 of 2 rooms taken
        when(referenceGenerator.generate(anyInt())).thenReturn(REF);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.createBooking(request());

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(saved.getValue().getGuestName()).isEqualTo("Asha Rao");
        assertThat(saved.getValue().getGuestEmail()).isEqualTo("asha@example.com");
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("15000.00"); // Mon-Thu, 3 weekday nights
        assertThat(response.bookingReference()).isEqualTo(REF);
        assertThat(response.price().totalAmount()).isEqualByComparingTo("15000.00");
        assertThat(response.nights()).isEqualTo(3);
    }

    @Test
    void rejectsBookingWhenNoRoomIsLeft() {
        when(hotelRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hotel));
        when(bookingRepository.countOverlappingConfirmed(1L, checkIn, checkOut)).thenReturn(2L); // 2 of 2 taken

        assertThatThrownBy(() -> service.createBooking(request()))
                .isInstanceOf(NoAvailabilityException.class)
                .hasMessageContaining("no rooms are available");
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsBookingWhenAlreadyOverbooked() {
        when(hotelRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hotel));
        when(bookingRepository.countOverlappingConfirmed(1L, checkIn, checkOut)).thenReturn(5L);

        assertThatThrownBy(() -> service.createBooking(request())).isInstanceOf(NoAvailabilityException.class);
    }

    @Test
    void unknownHotelIsNotFound() {
        when(hotelRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBooking(request())).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- lookup ----

    @Test
    void lookupNormalizesReferenceAndEmail() {
        Booking booking = existingBooking(BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingReferenceAndGuestEmailIgnoreCase(REF, "asha@example.com"))
                .thenReturn(Optional.of(booking));

        BookingResponse response = service.lookup(new BookingLookupRequest(" tc-2026-abc234 ", " Asha@Example.com "));

        assertThat(response.bookingReference()).isEqualTo(REF);
        assertThat(response.cancellable()).isTrue();
        assertThat(response.price().totalAmount()).isEqualByComparingTo("15000.00");
    }

    @Test
    void lookupWithWrongEmailIsNotFound() {
        when(bookingRepository.findByBookingReferenceAndGuestEmailIgnoreCase(REF, "other@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookup(new BookingLookupRequest(REF, "other@example.com")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- cancel ----

    @Test
    void cancelMarksBookingCancelledAndStampsTime() {
        Booking booking = existingBooking(BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingReferenceAndGuestEmailIgnoreCase(REF, "asha@example.com"))
                .thenReturn(Optional.of(booking));

        BookingResponse response = service.cancel(new BookingLookupRequest(REF, "asha@example.com"));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isNotNull();
        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.cancellable()).isFalse();
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancellingTwiceIsAConflict() {
        Booking booking = existingBooking(BookingStatus.CANCELLED);
        when(bookingRepository.findByBookingReferenceAndGuestEmailIgnoreCase(REF, "asha@example.com"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancel(new BookingLookupRequest(REF, "asha@example.com")))
                .isInstanceOf(InvalidBookingStateException.class)
                .hasMessageContaining("already cancelled");
        verify(bookingRepository, never()).save(any());
    }
}
