package com.aurapay.webhook.service;

import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.WebhookSubscription;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.dto.request.WebhookSubscriptionRequest;
import com.aurapay.webhook.dto.response.WebhookDeliveryResponse;
import com.aurapay.webhook.dto.response.WebhookSubscriptionResponse;
import com.aurapay.webhook.repository.WebhookDeliveryRepository;
import com.aurapay.webhook.repository.WebhookSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WebhookServiceTest {

    @Mock
    private WebhookSubscriptionRepository subscriptionRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private WebhookDispatcherService dispatcherService;

    @InjectMocks
    private WebhookService webhookService;

    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Creazione sottoscrizione webhook salva l'URL target e genera secret se non fornito")
    void createOrUpdateSubscription_success() {
        WebhookSubscriptionRequest req = new WebhookSubscriptionRequest(
                merchantId,
                "https://merchant.example.com/webhook",
                null,
                true,
                null
        );

        given(subscriptionRepository.findByMerchantId(merchantId)).willReturn(Optional.empty());
        given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WebhookSubscriptionResponse response = webhookService.createOrUpdateSubscription(req);

        assertThat(response.merchantId()).isEqualTo(merchantId);
        assertThat(response.targetUrl()).isEqualTo("https://merchant.example.com/webhook");
        assertThat(response.secretKey()).startsWith("whsec_");
        assertThat(response.enabled()).isTrue();
    }

    @Test
    @DisplayName("Replay manuale di una notifica azzera il conteggio tentativi ed esegue il dispatch")
    void replayDelivery_resetsAttemptsAndDispatches() {
        UUID deliveryId = UUID.randomUUID();
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(deliveryId)
                .eventId("evt_999")
                .merchantId(merchantId)
                .eventType("payment.succeeded")
                .targetUrl("https://merchant.example.com/webhook")
                .payload("{}")
                .attemptCount(5)
                .maxAttempts(5)
                .status(DeliveryStatus.DEAD_LETTER)
                .createdAt(Instant.now())
                .build();

        given(deliveryRepository.findById(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WebhookDeliveryResponse response = webhookService.replayDelivery(deliveryId);

        assertThat(response.attemptCount()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(DeliveryStatus.PENDING);

        verify(dispatcherService).dispatchDelivery(delivery);
    }
}
