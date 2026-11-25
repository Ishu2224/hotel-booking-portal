package com.thomascook.hotelbooking.dto.request;

import com.thomascook.hotelbooking.dto.validation.StayDates;
import com.thomascook.hotelbooking.dto.validation.ValidStayDates;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Query params of the per-hotel availability and price-quote endpoints: both dates required. */
@ValidStayDates
public record StayRequest(
        @NotNull(message = "Check-in date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
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
