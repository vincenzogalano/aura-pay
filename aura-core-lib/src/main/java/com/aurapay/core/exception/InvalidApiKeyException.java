package com.aurapay.core.exception;

public class InvalidApiKeyException extends BusinessException {

    public InvalidApiKeyException() {
        super(AuraErrorCode.INVALID_API_KEY, "The provided API key is invalid, revoked, or expired");
    }

    public InvalidApiKeyException(String message) {
        super(AuraErrorCode.INVALID_API_KEY, message);
    }
}
