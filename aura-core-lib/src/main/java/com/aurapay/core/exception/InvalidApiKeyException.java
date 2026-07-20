package com.aurapay.core.exception;

public class InvalidApiKeyException extends UnauthorizedException {

    public InvalidApiKeyException() {
        super("The provided API key is invalid, revoked, or expired");
    }

    public InvalidApiKeyException(String message) {
        super(message);
    }
}
