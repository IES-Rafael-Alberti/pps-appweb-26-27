package com.vetclinic.common;

/** Thrown when a request breaks a domain rule. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
