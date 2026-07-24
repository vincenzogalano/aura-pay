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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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

    @MockBean
    private MerchantService merchantService;

    @Test
    @DisplayName("POST /v1/merchants/register restituisce 201 CREATED e payload di registrazione")
    void registerMerchant_returns201Created() throws Exception {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .businessName("Test Store")
                .vatNumber("12345678901")
                .email("test@store.com")
                .build();

        UUID merchantId = UUID.randomUUID();
        MerchantResponse merchantResponse = MerchantResponse.builder()
                .id(merchantId)
                .businessName("Test Store")
                .vatNumber("12345678901")
                .email("test@store.com")
                .status(MerchantStatus.PENDING_VERIFICATION)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        RegisterMerchantResponse registerResponse = RegisterMerchantResponse.builder()
                .merchant(merchantResponse)
                .testApiKeys(List.of())
                .message("Merchant registered successfully.")
                .build();

        given(merchantService.registerMerchant(any())).willReturn(registerResponse);

        mockMvc.perform(post("/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchant.businessName").value("Test Store"))
                .andExpect(jsonPath("$.merchant.status").value("PENDING_VERIFICATION"));
    }
}
