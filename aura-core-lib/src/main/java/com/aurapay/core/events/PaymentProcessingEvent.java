package com.aurapay.core.events;

import java.time.Instant;

public record PaymentProcessingEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String paymentIntentId,
        String merchantId,
        long amountCents,
        String currency,
        boolean isTest
) implements DomainEvent {

    public PaymentProcessingEvent {
        if (eventType == null) {
            eventType = EventType.PAYMENT_PROCESSING.getTopicName();
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
