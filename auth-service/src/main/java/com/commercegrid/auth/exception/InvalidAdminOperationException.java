package com.commercegrid.auth.exception;

public class InvalidAdminOperationException extends RuntimeException {
    public InvalidAdminOperationException(String message) {
        super(message);
    }
}