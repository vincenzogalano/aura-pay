package com.aurapay.webhook.controller;

import com.aurapay.webhook.dto.request.WebhookSubscriptionRequest;
import com.aurapay.webhook.dto.response.WebhookSubscriptionResponse;
import com.aurapay.webhook.service.WebhookService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@ActiveProfiles("test")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebhookService webhookService;

    @Test
    @DisplayName("POST /v1/webhooks/subscriptions restituisce 201 CREATED e la sottoscrizione creata")
    void createSubscription_returns201Created() throws Exception {
        UUID merchantId = UUID.randomUUID();
        WebhookSubscriptionRequest request = WebhookSubscriptionRequest.builder()
                .merchantId(merchantId)
                .targetUrl("https://merchant.example.com/webhook")
                .build();

        WebhookSubscriptionResponse response = WebhookSubscriptionResponse.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .targetUrl("https://merchant.example.com/webhook")
                .secretKey("whsec_123456")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(webhookService.createOrUpdateSubscription(any())).willReturn(response);

        mockMvc.perform(post("/v1/webhooks/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetUrl").value("https://merchant.example.com/webhook"))
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
