package com.thomascook.hotelbooking.exception;

import org.springframework.http.HttpStatus;

/** 404 - hotel or booking not found (a wrong email on a lookup is deliberately indistinguishable). */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
