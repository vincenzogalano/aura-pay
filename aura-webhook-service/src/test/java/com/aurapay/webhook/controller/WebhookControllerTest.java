package com.aurapay.webhook.controller;

import com.aurapay.webhook.dto.request.WebhookSubscriptionRequest;
import com.aurapay.webhook.dto.response.WebhookSubscriptionResponse;
import com.aurapay.webhook.service.WebhookService;
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

    @MockitoBean
    private WebhookService webhookService;

    @Test
    @DisplayName("POST /v1/webhooks/subscriptions restituisce 201 CREATED e la sottoscrizione creata")
    void createSubscription_returns201Created() throws Exception {
        UUID merchantId = UUID.randomUUID();
        WebhookSubscriptionRequest request = new WebhookSubscriptionRequest(
                merchantId,
                "https://merchant.example.com/webhook",
                null,
                null,
                null
        );

        WebhookSubscriptionResponse response = new WebhookSubscriptionResponse(
                UUID.randomUUID(),
                merchantId,
                "https://merchant.example.com/webhook",
                "whsec_123456",
                true,
                null,
                Instant.now(),
                Instant.now()
        );

        given(webhookService.createOrUpdateSubscription(any())).willReturn(response);

        mockMvc.perform(post("/v1/webhooks/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetUrl").value("https://merchant.example.com/webhook"))
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
