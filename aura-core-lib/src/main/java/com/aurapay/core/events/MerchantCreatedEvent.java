package com.aurapay.core.events;

import java.time.Instant;

public record MerchantCreatedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String merchantId,
        String businessName,
        String vatNumber,
        String email,
        boolean isTest
) implements DomainEvent {

    public MerchantCreatedEvent {
        if (eventType == null) {
            eventType = EventType.MERCHANT_CREATED.getTopicName();
        }
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean isTest() {
        return isTest;
    }
}
