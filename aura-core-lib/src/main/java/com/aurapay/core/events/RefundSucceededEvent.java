package com.aurapay.core.events;

import java.time.Instant;

public record RefundSucceededEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String refundId,
        String paymentIntentId,
        String merchantId,
        long amountCents,
        String reason,
        boolean isTest
) implements DomainEvent {

    public RefundSucceededEvent {
        if (eventType == null) {
            eventType = EventType.REFUND_SUCCEEDED.getTopicName();
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
