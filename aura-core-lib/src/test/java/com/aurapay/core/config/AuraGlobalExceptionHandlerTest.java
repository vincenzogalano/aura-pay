package com.aurapay.core.config;

import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.exception.DomainRuleViolationException;
import com.aurapay.core.exception.ErrorResponse;
import com.aurapay.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuraGlobalExceptionHandlerTest {

    private final AuraGlobalExceptionHandler handler = new AuraGlobalExceptionHandler();

    @Test
    @DisplayName("Should handle ResourceNotFoundException and return HTTP 404")
    void shouldHandleResourceNotFoundException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/merchants/mch_999");
        ResourceNotFoundException ex = new ResourceNotFoundException("Merchant mch_999 not found");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Merchant mch_999 not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/merchants/mch_999");
    }

    @Test
    @DisplayName("Should handle DomainRuleViolationException and return HTTP 422")
    void shouldHandleDomainRuleViolationException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payments");
        DomainRuleViolationException ex = new DomainRuleViolationException("Payment amount must be positive");

        ResponseEntity<ErrorResponse> response = handler.handleDomainRuleViolationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(422);
        assertThat(response.getBody().error()).isEqualTo("DOMAIN_RULE_VIOLATION");
        assertThat(response.getBody().message()).isEqualTo("Payment amount must be positive");
    }

    @Test
    @DisplayName("Should handle BusinessException and return HTTP 422 and custom error code")
    void shouldHandleBusinessException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payouts");
        BusinessException ex = new BusinessException("PAYOUT_CONFLICT", "Payout already processed");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(422);
        assertThat(response.getBody().error()).isEqualTo("PAYOUT_CONFLICT");
        assertThat(response.getBody().message()).isEqualTo("Payout already processed");
    }

    @Test
    @DisplayName("Should handle generic Exception and return HTTP 500")
    void shouldHandleGenericException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        Exception ex = new RuntimeException("Database failure");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).isEqualTo("An unexpected internal error occurred");
    }
}
