package com.aurapay.merchant.exception;

import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.exception.ErrorResponse;
import com.aurapay.core.exception.FieldErrorResponse;
import com.aurapay.core.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("merchantGlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business Exception: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error on request path {}", request.getRequestURI());
        List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldErrorResponse(err.getField(), err.getDefaultMessage(), err.getRejectedValue()))
                .toList();

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                AuraErrorCode.VALIDATION_FAILED.getCode(),
                "Validation failed for request parameters",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at path {}", request.getRequestURI(), ex);
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                AuraErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "An unexpected internal server error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private HttpStatus determineHttpStatus(String errorCodeStr) {
        if (errorCodeStr == null) {
            return HttpStatus.BAD_REQUEST;
        }
        try {
            AuraErrorCode errorCode = AuraErrorCode.valueOf(errorCodeStr);
            return switch (errorCode) {
                case UNAUTHORIZED, INVALID_API_KEY -> HttpStatus.UNAUTHORIZED;
                case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
                case IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
                case DOMAIN_RULE_VIOLATION -> HttpStatus.UNPROCESSABLE_ENTITY;
                case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.BAD_REQUEST;
            };
        } catch (IllegalArgumentException e) {
            return HttpStatus.BAD_REQUEST;
        }
    }
}
