package com.thomascook.hotelbooking.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Single source of "now" for business rules. All hotels are in India, so "today" for the
 * check-in rule is evaluated in IST regardless of the server's default time zone.
 */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private AppTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
