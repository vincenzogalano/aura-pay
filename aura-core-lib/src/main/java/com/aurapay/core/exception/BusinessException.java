package com.aurapay.core.exception;

/**
 * Base exception for business logic errors in AuraPay domain.
 */
public class BusinessException extends AuraException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
