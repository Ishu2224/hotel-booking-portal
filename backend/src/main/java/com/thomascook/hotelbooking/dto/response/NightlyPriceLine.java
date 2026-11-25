package com.thomascook.hotelbooking.dto.response;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

/** One night of a stay. {@code date} is the night starting that day (check-out day is not charged). */
public record NightlyPriceLine(
        LocalDate date,
        DayOfWeek dayOfWeek,
        boolean weekendNight,
        BigDecimal basePrice,
        BigDecimal surcharge,
        BigDecimal price) {
}
