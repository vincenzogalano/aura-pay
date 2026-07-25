package com.aurapay.webhook.service;

import com.aurapay.core.constants.AuraHeaders;
import com.aurapay.core.security.HmacUtils;
import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.WebhookSubscription;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.publisher.WebhookEventPublisher;
import com.aurapay.webhook.repository.WebhookDeliveryRepository;
import com.aurapay.webhook.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcherService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookEventPublisher eventPublisher;

    private final RestClient restClient = RestClient.create();

    @Value("${aurapay.webhook.initial-backoff-seconds:5}")
    private int initialBackoffSeconds;

    @Value("${aurapay.webhook.backoff-multiplier:3}")
    private int backoffMultiplier;

    public void dispatchDelivery(WebhookDelivery delivery) {
        Optional<WebhookSubscription> subOpt = subscriptionRepository.findByMerchantId(delivery.getMerchantId());
        String secretKey = subOpt.map(WebhookSubscription::getSecretKey).orElse("whsec_default");

        int currentAttempt = delivery.getAttemptCount() + 1;
        delivery.setAttemptCount(currentAttempt);

        long timestamp = Instant.now().getEpochSecond();
        String signature = HmacUtils.calculateHmacSha256(timestamp + "." + delivery.getPayload(), secretKey);

        log.info("Dispatching webhook attempt {}/{} for eventId={} merchantId={} targetUrl={}",
                currentAttempt, delivery.getMaxAttempts(), delivery.getEventId(), delivery.getMerchantId(), delivery.getTargetUrl());

        try {
            var responseSpec = restClient.post()
                    .uri(delivery.getTargetUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(AuraHeaders.X_AURA_SIGNATURE, signature)
                    .header("X-Aura-Timestamp", String.valueOf(timestamp))
                    .header("X-Aura-Event-Id", delivery.getEventId())
                    .header("X-Aura-Event-Type", delivery.getEventType())
                    .body(delivery.getPayload())
                    .retrieve();

            responseSpec.toBodilessEntity();

            delivery.setHttpStatus(200);
            delivery.setResponseBody("OK");
            delivery.setStatus(DeliveryStatus.SUCCESS);
            delivery.setDeliveredAt(Instant.now());
            delivery.setNextRetryAt(null);
            deliveryRepository.save(delivery);

            log.info("Webhook delivery succeeded for deliveryId={}", delivery.getId());
            eventPublisher.publishDeliverySucceeded(
                    delivery.getId().toString(),
                    delivery.getMerchantId().toString(),
                    delivery.getTargetUrl(),
                    delivery.getEventType(),
                    200,
                    currentAttempt,
                    delivery.isTest()
            );

        } catch (Exception ex) {
            log.warn("Webhook delivery attempt {} failed for deliveryId={}: {}", currentAttempt, delivery.getId(), ex.getMessage());

            int statusCode = 500;
            if (ex instanceof HttpStatusCodeException httpEx) {
                statusCode = httpEx.getStatusCode().value();
            }
            delivery.setHttpStatus(statusCode);
            delivery.setResponseBody(ex.getMessage() != null ? ex.getMessage() : "Dispatch Exception");

            if (currentAttempt >= delivery.getMaxAttempts()) {
                delivery.setStatus(DeliveryStatus.DEAD_LETTER);
                delivery.setNextRetryAt(null);
                deliveryRepository.save(delivery);

                log.error("Webhook delivery REACHED MAX ATTEMPTS ({}), moved to DEAD_LETTER for deliveryId={}", currentAttempt, delivery.getId());
                eventPublisher.publishDeliveryDeadLettered(
                        delivery.getId().toString(),
                        delivery.getMerchantId().toString(),
                        delivery.getTargetUrl(),
                        delivery.getEventType(),
                        currentAttempt,
                        ex.getMessage(),
                        delivery.isTest()
                );
            } else {
                delivery.setStatus(DeliveryStatus.FAILED);
                long backoffSeconds = calculateBackoff(currentAttempt);
                delivery.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds));
                deliveryRepository.save(delivery);

                log.info("Scheduled retry for deliveryId={} in {} seconds (at {})", delivery.getId(), backoffSeconds, delivery.getNextRetryAt());
            }
        }
    }

    private long calculateBackoff(int attempt) {
        long backoff = initialBackoffSeconds;
        for (int i = 1; i < attempt; i++) {
            backoff *= backoffMultiplier;
        }
        return backoff;
    }
}
