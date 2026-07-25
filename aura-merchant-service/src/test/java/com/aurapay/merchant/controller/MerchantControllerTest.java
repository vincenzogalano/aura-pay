package com.aurapay.merchant.controller;

import com.aurapay.merchant.domain.enums.MerchantStatus;
import com.aurapay.merchant.dto.request.RegisterMerchantRequest;
import com.aurapay.merchant.dto.response.MerchantResponse;
import com.aurapay.merchant.dto.response.RegisterMerchantResponse;
import com.aurapay.merchant.service.MerchantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MerchantController.class)
@ActiveProfiles("test")
class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    @DisplayName("POST /v1/merchants/register restituisce 201 CREATED e payload di registrazione")
    void registerMerchant_returns201Created() throws Exception {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Test Store",
                "12345678901",
                "test@store.com"
        );

        UUID merchantId = UUID.randomUUID();
        MerchantResponse merchantResponse = new MerchantResponse(
                merchantId,
                "Test Store",
                "12345678901",
                "test@store.com",
                MerchantStatus.PENDING_VERIFICATION,
                Instant.now(),
                Instant.now()
        );

        RegisterMerchantResponse registerResponse = new RegisterMerchantResponse(
                merchantResponse,
                List.of(),
                "Merchant registered successfully."
        );

        given(merchantService.registerMerchant(any())).willReturn(registerResponse);

        mockMvc.perform(post("/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchant.businessName").value("Test Store"))
                .andExpect(jsonPath("$.merchant.status").value("PENDING_VERIFICATION"));
    }
}
