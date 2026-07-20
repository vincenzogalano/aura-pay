package com.aurapay.core.events;

import java.time.Instant;

public record ApiKeyRevokedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String apiKeyId,
        String merchantId,
        String keyPrefix,
        boolean isTest
) implements DomainEvent {

    public ApiKeyRevokedEvent {
        if (eventType == null) {
            eventType = EventType.API_KEY_REVOKED.getTopicName();
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
