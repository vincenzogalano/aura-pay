package com.aurapay.core.events;

import java.time.Instant;

public record PaymentIntentCreatedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String paymentIntentId,
        String merchantId,
        long amountCents,
        String currency,
        String idempotencyKey,
        boolean isTest
) implements DomainEvent {

    public PaymentIntentCreatedEvent {
        if (eventType == null) {
            eventType = EventType.PAYMENT_INTENT_CREATED.getTopicName();
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
