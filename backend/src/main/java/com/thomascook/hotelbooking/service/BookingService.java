package com.thomascook.hotelbooking.service;

import com.thomascook.hotelbooking.config.AppTime;
import com.thomascook.hotelbooking.dto.request.BookingLookupRequest;
import com.thomascook.hotelbooking.dto.request.CreateBookingRequest;
import com.thomascook.hotelbooking.dto.response.BookingResponse;
import com.thomascook.hotelbooking.dto.response.PriceBreakdownResponse;
import com.thomascook.hotelbooking.entity.Booking;
import com.thomascook.hotelbooking.entity.BookingStatus;
import com.thomascook.hotelbooking.entity.Hotel;
import com.thomascook.hotelbooking.exception.InvalidBookingStateException;
import com.thomascook.hotelbooking.exception.NoAvailabilityException;
import com.thomascook.hotelbooking.exception.ResourceNotFoundException;
import com.thomascook.hotelbooking.repository.BookingRepository;
import com.thomascook.hotelbooking.repository.HotelRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final PricingService pricingService;
    private final AvailabilityService availabilityService;
    private final BookingReferenceGenerator referenceGenerator;

    /**
     * Creates a booking. Concurrency: the hotel row is locked (SELECT ... FOR UPDATE) for the rest of this
     * transaction, so two requests for the same hotel run this method one after the other. The second one
     * therefore counts the first one's committed booking and gets a 409 if the last room is gone.
     * The price is always computed here; the client never sends one.
     * <p>Order matters: the lock must be the FIRST statement. On MySQL (REPEATABLE READ) the snapshot used by the
     * availability count is created at the first non-locking read, so counting only after the lock is held
     * guarantees the count includes every booking committed by the request we waited for.
     */
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        Hotel hotel = hotelRepository.findByIdForUpdate(request.hotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found."));

        int available = availabilityService.availableRooms(hotel, request.checkInDate(), request.checkOutDate());
        if (available <= 0) {
            throw new NoAvailabilityException("Sorry, no rooms are available at " + hotel.getName()
                    + " for the selected dates. Please choose different dates.");
        }

        PriceBreakdownResponse price = pricingService.quote(hotel, request.checkInDate(), request.checkOutDate());
        LocalDateTime now = AppTime.now();

        Booking booking = Booking.builder()
                .bookingReference(newUniqueReference(now.getYear()))
                .hotel(hotel)
                .guestName(request.guestName().trim())
                .guestEmail(normalizeEmail(request.guestEmail()))
                .checkInDate(request.checkInDate())
                .checkOutDate(request.checkOutDate())
                .nights(price.nights())
                .pricePerNight(price.pricePerNight())
                .weekendSurchargePercent(price.weekendSurchargePercent())
                .totalAmount(price.totalAmount())
                .status(BookingStatus.CONFIRMED)
                .createdAt(now)
                .build();

        Booking saved = bookingRepository.saveAndFlush(booking);
        log.info("Booking {} confirmed for hotel {} ({} night(s))", saved.getBookingReference(), hotel.getId(), price.nights());
        return BookingResponse.from(saved, price);
    }

    /** Both reference and email must match; a wrong email looks exactly like an unknown reference. */
    @Transactional(readOnly = true)
    public BookingResponse lookup(BookingLookupRequest request) {
        return toResponse(findBooking(request));
    }

    /** Cancels a CONFIRMED booking. The room is released at once because availability only counts CONFIRMED rows. */
    @Transactional
    public BookingResponse cancel(BookingLookupRequest request) {
        Booking booking = findBooking(request);
        if (!booking.isCancellable()) {
            throw new InvalidBookingStateException(
                    "Booking " + booking.getBookingReference() + " is already cancelled.");
        }
        booking.cancel(AppTime.now());
        bookingRepository.save(booking);
        log.info("Booking {} cancelled", booking.getBookingReference());
        return toResponse(booking);
    }

    private Booking findBooking(BookingLookupRequest request) {
        String reference = request.bookingReference().trim().toUpperCase(Locale.ROOT);
        String email = normalizeEmail(request.guestEmail());
        return bookingRepository.findByBookingReferenceAndGuestEmailIgnoreCase(reference, email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No booking found for that reference and email address."));
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.from(booking, pricingService.forBooking(booking));
    }

    private String newUniqueReference(int year) {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String reference = referenceGenerator.generate(year);
            if (!bookingRepository.existsByBookingReference(reference)) {
                return reference;
            }
        }
        throw new IllegalStateException("Could not generate a unique booking reference");
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
