package com.vetclinic.common;

/** Thrown when an authenticated user is not allowed to touch a resource (HTTP 403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
