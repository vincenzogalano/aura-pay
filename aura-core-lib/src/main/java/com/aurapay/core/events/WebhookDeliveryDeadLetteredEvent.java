package com.aurapay.core.events;

import java.time.Instant;

public record WebhookDeliveryDeadLetteredEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String deliveryId,
        String merchantId,
        String targetUrl,
        String originalEventType,
        int totalAttempts,
        String lastErrorReason,
        boolean isTest
) implements DomainEvent {

    public WebhookDeliveryDeadLetteredEvent {
        if (eventType == null) {
            eventType = EventType.WEBHOOK_DELIVERY_DEAD_LETTERED.getTopicName();
        }
    }

    @Override
    public String getEventId() { return eventId; }
    @Override
    public String getEventType() { return eventType; }
    @Override
    public Instant getOccurredAt() { return occurredAt; }
    @Override
    public boolean isTest() { return isTest; }
}
