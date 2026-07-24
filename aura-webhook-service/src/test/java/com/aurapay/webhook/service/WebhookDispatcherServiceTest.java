package com.aurapay.webhook.service;

import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.WebhookSubscription;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.publisher.WebhookEventPublisher;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WebhookDispatcherServiceTest {

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private WebhookSubscriptionRepository subscriptionRepository;

    @Mock
    private WebhookEventPublisher eventPublisher;

    @InjectMocks
    private WebhookDispatcherService dispatcherService;

    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        ReflectionTestUtils.setField(dispatcherService, "initialBackoffSeconds", 5);
        ReflectionTestUtils.setField(dispatcherService, "backoffMultiplier", 3);
    }

    @Test
    @DisplayName("Fallimento invio notifica calcola backoff esponenziale e schedula prossimo tentativo")
    void dispatchDelivery_failedAttempt_schedulesRetry() {
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(UUID.randomUUID())
                .eventId("evt_123")
                .merchantId(merchantId)
                .eventType("payment.succeeded")
                .targetUrl("http://unreachable-host.local/webhook") // Invalid domain to force failure
                .payload("{\"test\":true}")
                .attemptCount(0)
                .maxAttempts(5)
                .status(DeliveryStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        given(subscriptionRepository.findByMerchantId(merchantId))
                .willReturn(Optional.of(WebhookSubscription.builder()
                        .merchantId(merchantId)
                        .targetUrl("http://unreachable-host.local/webhook")
                        .secretKey("whsec_secret123")
                        .enabled(true)
                        .build()));

        given(deliveryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        dispatcherService.dispatchDelivery(delivery);

        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("Consegna che raggiunge il massimo numero di tentativi passa allo stato DEAD_LETTER ed emette evento")
    void dispatchDelivery_maxAttemptsReached_movesToDeadLetter() {
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(UUID.randomUUID())
                .eventId("evt_123")
                .merchantId(merchantId)
                .eventType("payment.succeeded")
                .targetUrl("http://unreachable-host.local/webhook")
                .payload("{\"test\":true}")
                .attemptCount(4) // 4 attempt already done, this is 5th
                .maxAttempts(5)
                .status(DeliveryStatus.FAILED)
                .createdAt(Instant.now())
                .build();

        given(subscriptionRepository.findByMerchantId(merchantId))
                .willReturn(Optional.of(WebhookSubscription.builder()
                        .merchantId(merchantId)
                        .targetUrl("http://unreachable-host.local/webhook")
                        .secretKey("whsec_secret123")
                        .enabled(true)
                        .build()));

        given(deliveryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        dispatcherService.dispatchDelivery(delivery);

        assertThat(delivery.getAttemptCount()).isEqualTo(5);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DEAD_LETTER);
        assertThat(delivery.getNextRetryAt()).isNull();

        verify(eventPublisher).publishDeliveryDeadLettered(
                eq(delivery.getId().toString()),
                eq(merchantId.toString()),
                eq("http://unreachable-host.local/webhook"),
                eq("payment.succeeded"),
                eq(5),
                any(),
                eq(false)
        );
    }
}
