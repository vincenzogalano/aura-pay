package com.aurapay.banksimulator.exception;

import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.exception.ErrorResponse;
import com.aurapay.core.exception.FieldErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        HttpStatus status = AuraErrorCode.BANK_UNAVAILABLE.getCode().equals(ex.getErrorCode()) ? 
                HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_REQUEST;

        ErrorResponse response = ErrorResponse.of(
                status.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                AuraErrorCode.VALIDATION_FAILED.getCode(),
                "Invalid request payload",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                AuraErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
