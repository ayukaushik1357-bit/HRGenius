package com.hrgenius.exception;

/** Thrown when a record cannot be deleted because other records still use it (HTTP 409). */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}
