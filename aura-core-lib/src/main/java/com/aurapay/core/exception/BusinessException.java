package com.aurapay.core.exception;

/**
 * Base exception for business logic errors in AuraPay domain.
 */
public class BusinessException extends AuraException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = AuraErrorCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(AuraErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
    }

    public String getErrorCode() {
        return errorCode;
    }
}
