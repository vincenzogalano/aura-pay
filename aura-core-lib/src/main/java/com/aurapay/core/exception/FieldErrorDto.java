package com.aurapay.core.exception;

public record FieldErrorDto(
        String field,
        String message,
        Object rejectedValue
) {}
