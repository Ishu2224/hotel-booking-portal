package com.thomascook.hotelbooking.dto.request;

import com.thomascook.hotelbooking.dto.validation.StayDates;
import com.thomascook.hotelbooking.dto.validation.ValidStayDates;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Query params of GET /api/hotels. Everything is optional; dates must come as a pair. */
@ValidStayDates(datesOptional = true)
public record HotelSearchRequest(
        @Size(max = 100, message = "Location must be at most 100 characters")
        String location,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate checkInDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate checkOutDate) implements StayDates {

    // JavaBean-style getters: Spring reads the rejected value of cross-field violations by property name.
    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }
}
