package com.aurapay.core.events;

import java.time.Instant;

public record ApiKeyCreatedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String apiKeyId,
        String merchantId,
        String keyPrefix,
        String environment,
        boolean isTest
) implements DomainEvent {

    public ApiKeyCreatedEvent {
        if (eventType == null) {
            eventType = EventType.API_KEY_CREATED.getTopicName();
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
