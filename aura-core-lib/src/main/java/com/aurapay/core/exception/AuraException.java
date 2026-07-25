package com.aurapay.core.exception;
public class AuraException extends RuntimeException {

    public AuraException(String message) {
        super(message);
    }

    public AuraException(String message, Throwable cause) {
        super(message, cause);
    }
}
