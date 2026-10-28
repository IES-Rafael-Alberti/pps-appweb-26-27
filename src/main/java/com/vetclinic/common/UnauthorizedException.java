package com.vetclinic.common;

/** Thrown when the caller is not authenticated (bad credentials, expired or reused refresh token). */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
