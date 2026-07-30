package com.aurapay.core.config;

import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.exception.DomainRuleViolationException;
import com.aurapay.core.exception.ErrorResponse;
import com.aurapay.core.exception.FieldErrorResponse;
import com.aurapay.core.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class AuraGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuraGlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found [path={}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                AuraErrorCode.RESOURCE_NOT_FOUND.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleDomainRuleViolationException(
            DomainRuleViolationException ex, HttpServletRequest request) {
        log.warn("Domain rule violation [path={}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                AuraErrorCode.DOMAIN_RULE_VIOLATION.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception [path={}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed [path={}]: {}", request.getRequestURI(), ex.getMessage());

        List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorResponse(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                AuraErrorCode.VALIDATION_FAILED.getCode(),
                "Validation failed for one or more fields",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception [path={}]: ", request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                AuraErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "An unexpected internal error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
