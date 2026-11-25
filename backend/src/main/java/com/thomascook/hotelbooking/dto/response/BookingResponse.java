package com.thomascook.hotelbooking.dto.response;

import com.thomascook.hotelbooking.entity.Booking;
import com.thomascook.hotelbooking.entity.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        String bookingReference,
        BookingStatus status,
        boolean cancellable,
        Long hotelId,
        String hotelName,
        String hotelLocation,
        String roomType,
        String guestName,
        String guestEmail,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int nights,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        PriceBreakdownResponse price) {

    /** The booking's hotel association must already be loaded. */
    public static BookingResponse from(Booking booking, PriceBreakdownResponse price) {
        return new BookingResponse(
                booking.getBookingReference(),
                booking.getStatus(),
                booking.isCancellable(),
                booking.getHotel().getId(),
                booking.getHotel().getName(),
                booking.getHotel().getLocation(),
                booking.getHotel().getRoomType(),
                booking.getGuestName(),
                booking.getGuestEmail(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getNights(),
                booking.getCreatedAt(),
                booking.getCancelledAt(),
                price);
    }
}
