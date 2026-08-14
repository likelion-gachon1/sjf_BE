package com.sjf.portal.exception;

import org.springframework.http.HttpStatus;

public class PortalApiException extends RuntimeException {

    private final HttpStatus status;

    public PortalApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
