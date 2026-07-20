package com.aurapay.core.events;

import java.time.Instant;

public record BankAuthorizationResultEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String transactionId,
        String paymentIntentId,
        boolean authorized,
        String responseCode,
        String authorizationCode,
        String declineReason,
        boolean isTest
) implements DomainEvent {

    public BankAuthorizationResultEvent {
        if (eventType == null) {
            eventType = EventType.BANK_AUTHORIZATION_RESULT.getTopicName();
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
