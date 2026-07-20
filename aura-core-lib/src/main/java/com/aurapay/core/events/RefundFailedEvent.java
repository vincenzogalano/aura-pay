package com.aurapay.core.events;

import java.time.Instant;

public record RefundFailedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String refundId,
        String paymentIntentId,
        String merchantId,
        long amountCents,
        String failureReason,
        boolean isTest
) implements DomainEvent {

    public RefundFailedEvent {
        if (eventType == null) {
            eventType = EventType.REFUND_FAILED.getTopicName();
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
