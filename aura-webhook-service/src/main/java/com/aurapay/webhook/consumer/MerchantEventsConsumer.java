package com.aurapay.webhook.consumer;

import com.aurapay.core.events.EventType;
import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.WebhookSubscription;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.repository.WebhookDeliveryRepository;
import com.aurapay.webhook.repository.WebhookSubscriptionRepository;
import com.aurapay.webhook.service.WebhookDispatcherService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
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
    public void consumeDomainEvent(
            String rawMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        log.info("Received Kafka raw message on topic={}", topic);
        try {
            JsonNode node = objectMapper.readTree(rawMessage);

            String eventId = node.has("eventId") ? node.get("eventId").asText() : UUID.randomUUID().toString();
            String eventType = node.has("eventType") ? node.get("eventType").asText() : topic;
            boolean isTest = node.has("isTest") && node.get("isTest").asBoolean(true);

            UUID merchantId = extractMerchantId(node);
            if (merchantId == null) {
                log.warn("Could not extract merchantId from event topic={} payload={}", topic, rawMessage);
                return;
            }

            Optional<WebhookSubscription> subOpt = subscriptionRepository.findByMerchantId(merchantId);
            if (subOpt.isEmpty() || !subOpt.get().isEnabled()) {
                log.info("No active webhook subscription for merchantId={}. Skipping.", merchantId);
                return;
            }

            WebhookSubscription subscription = subOpt.get();

            WebhookDelivery delivery = WebhookDelivery.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .merchantId(merchantId)
                    .eventType(eventType)
                    .targetUrl(subscription.getTargetUrl())
                    .payload(rawMessage)
                    .attemptCount(0)
                    .maxAttempts(maxAttempts)
                    .status(DeliveryStatus.PENDING)
                    .createdAt(Instant.now())
                    .isTest(isTest)
                    .build();

            delivery = deliveryRepository.save(delivery);
            dispatcherService.dispatchDelivery(delivery);

        } catch (Exception e) {
            log.error("Failed to process Kafka message on topic={}: {}", topic, e.getMessage(), e);
        }
    }

    private UUID extractMerchantId(JsonNode node) {
        try {
            if (node.has("merchantId")) {
                return UUID.fromString(node.get("merchantId").asText());
            }
        } catch (Exception e) {
            log.error("Failed to extract merchantId: {}", e.getMessage());
        }
        return null;
    }
}
