package com.thomascook.hotelbooking.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Human-readable references such as {@code TC-2026-K7XM3P}. The random part avoids look-alike
 * characters (0/O, 1/I/L) so references can be read out over the phone.
 */
@Component
public class BookingReferenceGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int RANDOM_PART_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String generate(int year) {
        StringBuilder suffix = new StringBuilder(RANDOM_PART_LENGTH);
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return "TC-" + year + "-" + suffix;
    }
}
