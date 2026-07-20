package com.aurapay.core.events;

import java.time.Instant;

public record WebhookDeliverySucceededEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String deliveryId,
        String merchantId,
        String targetUrl,
        String originalEventType,
        int responseStatusCode,
        int attemptNumber,
        boolean isTest
) implements DomainEvent {

    public WebhookDeliverySucceededEvent {
        if (eventType == null) {
            eventType = EventType.WEBHOOK_DELIVERY_SUCCEEDED.getTopicName();
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
