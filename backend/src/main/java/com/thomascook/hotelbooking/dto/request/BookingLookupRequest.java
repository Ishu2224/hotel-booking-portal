package com.thomascook.hotelbooking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Proves the caller knows both the reference and the email. Sent in a POST body (not the URL)
 * so the email does not end up in access logs. Used for both lookup and cancellation.
 */
public record BookingLookupRequest(
        @NotBlank(message = "Booking reference is required")
        @Size(max = 20, message = "Booking reference is too long")
        String bookingReference,

        @NotBlank(message = "Guest email is required")
        @Email(regexp = ".+@.+\\..+", message = "Enter a valid email address")
        @Size(max = 254, message = "Guest email must be at most 254 characters")
        String guestEmail) {
}
