package com.aurapay.core.exception;

/**
 * Base unchecked exception for all AuraPay ecosystem domain and infrastructure exceptions.
 */
public class AuraException extends RuntimeException {

    public AuraException(String message) {
        super(message);
    }

    public AuraException(String message, Throwable cause) {
        super(message, cause);
    }
}
