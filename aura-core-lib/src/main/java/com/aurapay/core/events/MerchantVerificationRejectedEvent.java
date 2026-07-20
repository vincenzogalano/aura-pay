package com.aurapay.core.events;

import java.time.Instant;

public record MerchantVerificationRejectedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String merchantId,
        String rejectionReason,
        boolean isTest
) implements DomainEvent {

    public MerchantVerificationRejectedEvent {
        if (eventType == null) {
            eventType = EventType.MERCHANT_VERIFICATION_REJECTED.getTopicName();
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
