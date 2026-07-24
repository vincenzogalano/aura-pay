package com.aurapay.webhook.publisher;

import com.aurapay.core.events.EventType;
import com.aurapay.core.events.WebhookDeliveryDeadLetteredEvent;
import com.aurapay.core.events.WebhookDeliverySucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishDeliverySucceeded(
            String deliveryId,
            String merchantId,
            String targetUrl,
            String originalEventType,
            int statusCode,
            int attemptNumber,
            boolean isTest) {

        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
        WebhookDeliverySucceededEvent event = new WebhookDeliverySucceededEvent(
                eventId,
                EventType.WEBHOOK_DELIVERY_SUCCEEDED.getTopicName(),
                Instant.now(),
                deliveryId,
                merchantId,
                targetUrl,
                originalEventType,
                statusCode,
                attemptNumber,
                isTest
        );
        log.info("Publishing WebhookDeliverySucceededEvent: {}", event);
        kafkaTemplate.send(EventType.WEBHOOK_DELIVERY_SUCCEEDED.getTopicName(), merchantId, event);
    }

    public void publishDeliveryDeadLettered(
            String deliveryId,
            String merchantId,
            String targetUrl,
            String originalEventType,
            int totalAttempts,
            String lastErrorReason,
            boolean isTest) {

        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
        WebhookDeliveryDeadLetteredEvent event = new WebhookDeliveryDeadLetteredEvent(
                eventId,
                EventType.WEBHOOK_DELIVERY_DEAD_LETTERED.getTopicName(),
                Instant.now(),
                deliveryId,
                merchantId,
                targetUrl,
                originalEventType,
                totalAttempts,
                lastErrorReason,
                isTest
        );
        log.warn("Publishing WebhookDeliveryDeadLetteredEvent: {}", event);
        kafkaTemplate.send(EventType.WEBHOOK_DELIVERY_DEAD_LETTERED.getTopicName(), merchantId, event);
    }
}
