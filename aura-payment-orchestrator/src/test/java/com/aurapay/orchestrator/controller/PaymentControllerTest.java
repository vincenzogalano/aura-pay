package com.aurapay.orchestrator.controller;

import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.ResourceNotFoundException;
import com.aurapay.orchestrator.domain.enums.PaymentStatus;
import com.aurapay.orchestrator.dto.request.ConfirmPaymentIntentRequest;
import com.aurapay.orchestrator.dto.request.CreatePaymentIntentRequest;
import com.aurapay.orchestrator.dto.request.RefundPaymentRequest;
import com.aurapay.orchestrator.dto.response.PaymentIntentResponse;
import com.aurapay.orchestrator.exception.GlobalExceptionHandler;
import com.aurapay.orchestrator.service.PaymentOrchestrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentOrchestrationService paymentOrchestrationService;

    @Test
    @DisplayName("POST /v1/payments - Dovrebbe creare un nuovo PaymentIntent e restituire 201 Created")
    void createPaymentIntent_Success() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID intentId = UUID.randomUUID();
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(merchantId, 5000L, "EUR", "Test payment", "customer@test.com", true);
        PaymentIntentResponse response = new PaymentIntentResponse(
                intentId, merchantId, 5000L, "EUR", PaymentStatus.CREATED,
                "pi_secret_123", "Test payment", "customer@test.com", 0L, null, null, null, null, true, Instant.now(), Instant.now()
        );

        given(paymentOrchestrationService.createPaymentIntent(any(CreatePaymentIntentRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(intentId.toString()))
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.amountCents").value(5000))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerEmail").value("customer@test.com"));
    }

    @Test
    @DisplayName("POST /v1/payments - Dovrebbe restituire 400 Bad Request in caso di dati non validi")
    void createPaymentIntent_InvalidRequest() throws Exception {
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(null, -10L, "EUR", "Test", "customer@test.com", true);

        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(AuraErrorCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    @DisplayName("POST /v1/payments/{id}/confirm - Dovrebbe confermare un PaymentIntent con carta Luhn e restituire 200 OK")
    void confirmPayment_Success() throws Exception {
        UUID intentId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        String validLuhnToken = "tok_4111111111111111";
        ConfirmPaymentIntentRequest request = new ConfirmPaymentIntentRequest(validLuhnToken);

        PaymentIntentResponse response = new PaymentIntentResponse(
                intentId, merchantId, 5000L, "EUR", PaymentStatus.SUCCEEDED,
                "pi_secret_123", "Test payment", "customer@test.com", 0L, validLuhnToken, "AUTH_123456", "tx_bank_999", null, true, Instant.now(), Instant.now()
        );

        given(paymentOrchestrationService.confirmPayment(eq(intentId), any(ConfirmPaymentIntentRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/v1/payments/{id}/confirm", intentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(intentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.authorizationCode").value("AUTH_123456"))
                .andExpect(jsonPath("$.transactionId").value("tx_bank_999"));
    }

    @Test
    @DisplayName("GET /v1/payments/{id} - Dovrebbe restituire 200 OK ed i dettagli del PaymentIntent")
    void getPaymentById_Success() throws Exception {
        UUID intentId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        PaymentIntentResponse response = new PaymentIntentResponse(
                intentId, merchantId, 5000L, "EUR", PaymentStatus.CREATED,
                "pi_secret_123", "Test payment", "customer@test.com", 0L, null, null, null, null, true, Instant.now(), Instant.now()
        );

        given(paymentOrchestrationService.getPaymentById(intentId)).willReturn(response);

        mockMvc.perform(get("/v1/payments/{id}", intentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(intentId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("GET /v1/payments/{id} - Dovrebbe restituire 404 Not Found se il PaymentIntent non esiste")
    void getPaymentById_NotFound() throws Exception {
        UUID intentId = UUID.randomUUID();
        given(paymentOrchestrationService.getPaymentById(intentId))
                .willThrow(new ResourceNotFoundException("PaymentIntent with id '" + intentId + "' not found"));

        mockMvc.perform(get("/v1/payments/{id}", intentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(AuraErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("POST /v1/payments/{id}/refund - Dovrebbe rimborsare un PaymentIntent e restituire 200 OK")
    void refundPayment_Success() throws Exception {
        UUID intentId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        RefundPaymentRequest request = new RefundPaymentRequest(2000L, "Reso prodotto");

        PaymentIntentResponse response = new PaymentIntentResponse(
                intentId, merchantId, 5000L, "EUR", PaymentStatus.PARTIALLY_REFUNDED,
                "pi_secret_123", "Test payment", "customer@test.com", 2000L, "tok_123", "AUTH_123456", "tx_bank_999", null, true, Instant.now(), Instant.now()
        );

        given(paymentOrchestrationService.refundPayment(eq(intentId), any(RefundPaymentRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/v1/payments/{id}/refund", intentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(intentId.toString()))
                .andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.refundedAmountCents").value(2000));
    }
}
