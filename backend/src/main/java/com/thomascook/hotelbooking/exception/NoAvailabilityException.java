package com.thomascook.hotelbooking.exception;

import org.springframework.http.HttpStatus;

/** 409 - no room left for the requested dates (e.g. the loser of a last-room race). */
public class NoAvailabilityException extends ApiException {

    public NoAvailabilityException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
