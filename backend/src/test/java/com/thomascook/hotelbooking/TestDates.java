package com.thomascook.hotelbooking;

import com.thomascook.hotelbooking.config.AppTime;
import java.time.DayOfWeek;
import java.time.LocalDate;

/** Deterministic future dates for tests: pricing depends on the weekday, validation on "today". */
final class TestDates {

    private TestDates() {
    }

    /** First date with the given weekday that is at least 7 days from today (always valid for check-in). */
    static LocalDate next(DayOfWeek day) {
        LocalDate date = AppTime.today().plusDays(7);
        while (date.getDayOfWeek() != day) {
            date = date.plusDays(1);
        }
        return date;
    }
}
