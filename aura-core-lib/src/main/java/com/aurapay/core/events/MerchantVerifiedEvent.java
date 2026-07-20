package com.aurapay.core.events;

import java.time.Instant;

public record MerchantVerifiedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String merchantId,
        String businessName,
        String vatNumber,
        boolean isTest
) implements DomainEvent {

    public MerchantVerifiedEvent {
        if (eventType == null) {
            eventType = EventType.MERCHANT_VERIFIED.getTopicName();
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
