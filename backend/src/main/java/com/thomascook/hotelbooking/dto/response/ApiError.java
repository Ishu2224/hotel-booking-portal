package com.thomascook.hotelbooking.dto.response;

import java.time.Instant;
import java.util.List;

/** The one error shape returned by every failing endpoint. {@code fieldErrors} is empty for non-validation errors. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldErrorDetail> fieldErrors) {
}
