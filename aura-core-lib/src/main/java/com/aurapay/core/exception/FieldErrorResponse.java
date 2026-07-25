package com.aurapay.core.exception;

public record FieldErrorResponse(
        String field,
        String message,
        Object rejectedValue
) {}
