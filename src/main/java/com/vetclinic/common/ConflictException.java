package com.vetclinic.common;

/** Thrown when a request conflicts with the current state of the domain. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
