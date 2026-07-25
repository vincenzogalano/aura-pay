package com.aurapay.webhook.consumer;

import com.aurapay.core.events.DomainEvent;
import com.aurapay.core.events.EventType;
import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.WebhookSubscription;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.repository.WebhookDeliveryRepository;
import com.aurapay.webhook.repository.WebhookSubscriptionRepository;
import com.aurapay.webhook.service.WebhookDispatcherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantEventsConsumer {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcherService dispatcherService;
    private final ObjectMapper objectMapper;

    @Value("${aurapay.webhook.max-retry-attempts:5}")
    private int maxAttempts;

    @KafkaListener(
            topics = {
                    EventType.Topics.PAYMENT_SUCCEEDED,
                    EventType.Topics.REFUND_SUCCEEDED,
                    EventType.Topics.MERCHANT_VERIFIED,
                    EventType.Topics.MERCHANT_VERIFICATION_REJECTED,
                    EventType.Topics.INVOICE_GENERATED,
                    EventType.Topics.PAYMENT_FAILED,
                    EventType.Topics.REFUND_FAILED
            },
            groupId = "webhook-service-group"
    )
    public void consumeDomainEvent(DomainEvent event) {
        log.info("Received Kafka event type={} id={}", event.getEventType(), event.getEventId());

        try {
            UUID merchantId = extractMerchantId(event);
            if (merchantId == null) {
                log.warn("Could not extract merchantId from event {}", event);
                return;
            }

            Optional<WebhookSubscription> subOpt = subscriptionRepository.findByMerchantId(merchantId);
            if (subOpt.isEmpty() || !subOpt.get().isEnabled()) {
                log.info("No active webhook subscription found for merchantId={}. Skipping webhook dispatch.", merchantId);
                return;
            }

            WebhookSubscription subscription = subOpt.get();
            String payloadJson = objectMapper.writeValueAsString(event);

            WebhookDelivery delivery = WebhookDelivery.builder()
                    .id(UUID.randomUUID())
                    .eventId(event.getEventId())
                    .merchantId(merchantId)
                    .eventType(event.getEventType())
                    .targetUrl(subscription.getTargetUrl())
                    .payload(payloadJson)
                    .attemptCount(0)
                    .maxAttempts(maxAttempts)
                    .status(DeliveryStatus.PENDING)
                    .createdAt(Instant.now())
                    .isTest(event.isTest())
                    .build();

            delivery = deliveryRepository.save(delivery);
            dispatcherService.dispatchDelivery(delivery);

        } catch (Exception e) {
            log.error("Failed to process consumed event {}", event.getEventId(), e);
        }
    }

    private UUID extractMerchantId(DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            var node = objectMapper.readTree(json);
            if (node.has("merchantId")) {
                return UUID.fromString(node.get("merchantId").asText());
            }
        } catch (Exception e) {
            log.error("Failed to extract merchantId from Kafka record payload: {}", e.getMessage());
        }
        return null;
    }
}
