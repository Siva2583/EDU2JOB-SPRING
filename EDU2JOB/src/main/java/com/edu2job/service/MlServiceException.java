package com.edu2job.service;

import org.springframework.http.HttpStatus;

/**
 * A failure in the Spring Boot → Flask ML call, carrying the HTTP status the
 * API should return to the frontend. The message is always safe to show to
 * users (no stack traces, internal URLs, or Flask internals).
 */
public class MlServiceException extends RuntimeException {

    private final HttpStatus status;

    public MlServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
