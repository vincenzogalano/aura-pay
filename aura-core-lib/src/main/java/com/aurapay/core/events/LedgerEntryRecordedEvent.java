package com.aurapay.core.events;

import java.time.Instant;

public record LedgerEntryRecordedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String entryId,
        String merchantId,
        String referenceType, // PAYMENT, REFUND, FEE
        String referenceId,
        long amountCents,
        String debitAccount,
        String creditAccount,
        boolean isTest
) implements DomainEvent {

    public LedgerEntryRecordedEvent {
        if (eventType == null) {
            eventType = EventType.LEDGER_ENTRY_RECORDED.getTopicName();
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
