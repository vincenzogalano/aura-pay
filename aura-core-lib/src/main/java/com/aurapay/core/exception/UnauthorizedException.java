package com.aurapay.core.exception;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(AuraErrorCode.UNAUTHORIZED, message);
    }
}
