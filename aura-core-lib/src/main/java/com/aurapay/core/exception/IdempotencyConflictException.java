package com.aurapay.core.exception;

public class IdempotencyConflictException extends BusinessException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("IDEMPOTENCY_CONFLICT", String.format("A request with Idempotency-Key '%s' is currently being processed or was processed with different parameters", idempotencyKey));
    }
}
