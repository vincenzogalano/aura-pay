package com.aurapay.core.events;

import java.time.Instant;

public record InvoiceGeneratedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String invoiceId,
        String invoiceNumber,
        String merchantId,
        String paymentIntentId,
        long amountCents,
        String pdfObjectKey,
        boolean isTest
) implements DomainEvent {

    public InvoiceGeneratedEvent {
        if (eventType == null) {
            eventType = EventType.INVOICE_GENERATED.getTopicName();
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
