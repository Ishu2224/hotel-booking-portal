package com.thomascook.hotelbooking.exception;

import org.springframework.http.HttpStatus;

/** 409 - the booking is not in a state that allows the operation (e.g. cancelling twice). */
public class InvalidBookingStateException extends ApiException {

    public InvalidBookingStateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
