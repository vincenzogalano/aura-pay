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
        String customerEmail,
        String description,
        boolean isTest
) implements DomainEvent {

    public PaymentSucceededEvent {
        if (eventType == null) {
            eventType = EventType.PAYMENT_SUCCEEDED.getTopicName();
        }
    }

    public PaymentSucceededEvent(
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
    ) {
        this(eventId, eventType, occurredAt, paymentIntentId, merchantId, amountCents, feeCents, currency, cardLastFour, authorizationCode, null, null, isTest);
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
