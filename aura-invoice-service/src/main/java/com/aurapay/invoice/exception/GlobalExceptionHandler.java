package com.aurapay.invoice.exception;

import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.AuraException;
import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.exception.ErrorResponse;
import com.aurapay.core.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Collections;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                AuraErrorCode.RESOURCE_NOT_FOUND.getCode(),
                ex.getMessage(),
                request.getRequestURI(),
                Collections.emptyList()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = AuraErrorCode.UNAUTHORIZED.getCode().equals(ex.getErrorCode()) ? HttpStatus.UNAUTHORIZED : HttpStatus.UNPROCESSABLE_ENTITY;
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                Collections.emptyList()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(AuraException.class)
    public ResponseEntity<ErrorResponse> handleAuraException(AuraException ex, HttpServletRequest request) {
        log.error("Aura exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                AuraErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ex.getMessage(),
                request.getRequestURI(),
                Collections.emptyList()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                AuraErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "An unexpected internal error occurred: " + ex.getMessage(),
                request.getRequestURI(),
                Collections.emptyList()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
