package com.aurapay.core.events;

import java.time.Instant;

public record PaymentFailedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String paymentIntentId,
        String merchantId,
        long amountCents,
        String currency,
        String failureCode,
        String failureMessage,
        boolean isTest
) implements DomainEvent {

    public PaymentFailedEvent {
        if (eventType == null) {
            eventType = EventType.PAYMENT_FAILED.getTopicName();
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
