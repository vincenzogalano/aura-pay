package com.aurapay.core.events;

import java.time.Instant;

public record PaymentSucceededEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String paymentIntentId,
        String merchantId,
        long amountCents,
        long feeCents,
        String currency,
        String cardLastFour,
        String authorizationCode,
        boolean isTest
) implements DomainEvent {

    public PaymentSucceededEvent {
        if (eventType == null) {
            eventType = EventType.PAYMENT_SUCCEEDED.getTopicName();
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
