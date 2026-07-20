package com.aurapay.core.events;

import java.time.Instant;

public record InvoiceGenerationFailedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String merchantId,
        String paymentIntentId,
        String errorMessage,
        boolean isTest
) implements DomainEvent {

    public InvoiceGenerationFailedEvent {
        if (eventType == null) {
            eventType = EventType.INVOICE_GENERATION_FAILED.getTopicName();
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
