package com.aurapay.vault.controller;

import com.aurapay.vault.domain.enums.CardBrand;
import com.aurapay.vault.dto.request.RetrieveRequest;
import com.aurapay.vault.dto.request.TokenizeRequest;
import com.aurapay.vault.dto.response.CardDetailsResponse;
import com.aurapay.vault.dto.response.TokenResponse;
import com.aurapay.vault.exception.GlobalExceptionHandler;
import com.aurapay.vault.service.VaultService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VaultController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Vault Controller Endpoints Integration Tests")
class VaultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VaultService vaultService;

    @Test
    @DisplayName("POST /v1/tokens - Should return 201 Created on successful tokenization")
    void tokenize_success() throws Exception {
        TokenizeRequest request = new TokenizeRequest(
                "4111111111111111", "John Doe", 12, 2030, "123"
        );
        TokenResponse response = new TokenResponse(
                "tok_test_123", "411111******1111", CardBrand.VISA, "John Doe", 12, 2030,
                Instant.now(), Instant.now().plusSeconds(900), false
        );

        given(vaultService.tokenize(any(), any())).willReturn(response);

        mockMvc.perform(post("/v1/tokens")
                        .header("Authorization", "Bearer pk_test_123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("tok_test_123"))
                .andExpect(jsonPath("$.maskedPan").value("411111******1111"))
                .andExpect(jsonPath("$.cardBrand").value("VISA"))
                .andExpect(jsonPath("$.cardholderName").value("John Doe"));
    }

    @Test
    @DisplayName("POST /v1/tokens - Should return 400 Bad Request on validation failure")
    void tokenize_validationError() throws Exception {

        TokenizeRequest request = new TokenizeRequest(
                "4111", "John Doe", 13, 2030, "1"
        );

        mockMvc.perform(post("/v1/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /v1/tokens/retrieve - Should return 200 OK with card details")
    void retrieve_success() throws Exception {
        RetrieveRequest request = new RetrieveRequest("tok_test_123");
        CardDetailsResponse response = new CardDetailsResponse(
                "4111111111111111", "John Doe", 12, 2030, "123",
                "411111******1111", CardBrand.VISA, false
        );

        given(vaultService.retrieve(eq("tok_test_123"), any())).willReturn(response);

        mockMvc.perform(post("/v1/tokens/retrieve")
                        .header("Authorization", "Bearer sk_test_123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                .andExpect(jsonPath("$.cvv").value("123"))
                .andExpect(jsonPath("$.cardholderName").value("John Doe"))
                .andExpect(jsonPath("$.cardBrand").value("VISA"));
    }
}
