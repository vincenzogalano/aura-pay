package com.aurapay.banksimulator.controller;

import com.aurapay.banksimulator.dto.request.BankAuthorizationRequest;
import com.aurapay.banksimulator.dto.response.BankAuthorizationResponse;
import com.aurapay.banksimulator.dto.request.BankRefundRequest;
import com.aurapay.banksimulator.dto.response.BankRefundResponse;
import com.aurapay.banksimulator.exception.GlobalExceptionHandler;
import com.aurapay.banksimulator.service.BankSimulatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankSimulatorController.class)
@Import(GlobalExceptionHandler.class)
class BankSimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankSimulatorService bankSimulatorService;

    private UUID paymentIntentId;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        paymentIntentId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /v1/bank/authorize - Should return 200 OK with approval payload")
    void authorize_success() throws Exception {
        BankAuthorizationRequest request = new BankAuthorizationRequest(
                paymentIntentId, merchantId, 5000L, "EUR", "tok_test_123", true
        );

        given(bankSimulatorService.authorize(any()))
                .willReturn(BankAuthorizationResponse.approved("tx_bank_12345", "AUTH_999888"));

        mockMvc.perform(post("/v1/bank/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorized").value(true))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.transactionId").value("tx_bank_12345"))
                .andExpect(jsonPath("$.authorizationCode").value("AUTH_999888"));
    }

    @Test
    @DisplayName("POST /v1/bank/authorize - Should return 400 Bad Request on invalid payload")
    void authorize_validationFailed() throws Exception {
        BankAuthorizationRequest request = new BankAuthorizationRequest(
                null, merchantId, -10L, "", "", true
        );

        mockMvc.perform(post("/v1/bank/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /v1/bank/refund - Should return 200 OK with refund payload")
    void refund_success() throws Exception {
        BankRefundRequest request = new BankRefundRequest(
                "tx_bank_12345", merchantId, 2000L, "customer_request"
        );

        given(bankSimulatorService.refund(any()))
                .willReturn(BankRefundResponse.ok("tx_ref_999888"));

        mockMvc.perform(post("/v1/bank/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.refundTransactionId").value("tx_ref_999888"));
    }
}
