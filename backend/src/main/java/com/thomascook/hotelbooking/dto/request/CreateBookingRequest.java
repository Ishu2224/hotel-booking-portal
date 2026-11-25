package com.thomascook.hotelbooking.dto.request;

import com.thomascook.hotelbooking.dto.validation.StayDates;
import com.thomascook.hotelbooking.dto.validation.ValidStayDates;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Body of POST /api/bookings. There is deliberately no price field: the backend always computes it. */
@ValidStayDates
public record CreateBookingRequest(
        @NotNull(message = "Hotel is required")
        Long hotelId,

        @NotNull(message = "Check-in date is required")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOutDate,

        @NotBlank(message = "Guest name is required")
        @Size(max = 100, message = "Guest name must be at most 100 characters")
        String guestName,

        @NotBlank(message = "Guest email is required")
        @Email(regexp = ".+@.+\\..+", message = "Enter a valid email address")
        @Size(max = 254, message = "Guest email must be at most 254 characters")
        String guestEmail) implements StayDates {

    // JavaBean-style getters: Spring reads the rejected value of cross-field violations by property name.
    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }
}
