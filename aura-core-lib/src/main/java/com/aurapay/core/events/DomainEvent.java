package com.aurapay.core.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

/**
 * Base interface for all domain events across the AuraPay ecosystem.
 */
public interface DomainEvent {

    /**
     * Unique identifier for this specific event instance (e.g., evt_xxxx or UUID).
     */
    String getEventId();

    /**
     * Type identifier matching EventType constant.
     */
    String getEventType();

    /**
     * Helper method to resolve the strongly typed EventType enum, or null if unrecognized.
     */
    @JsonIgnore
    default EventType getEventTypeEnum() {
        return EventType.fromTopicName(getEventType());
    }

    /**
     * Timestamp when the event occurred in UTC.
     */
    Instant getOccurredAt();

    /**
     * Flag indicating whether the event originated from a test/sandbox environment.
     */
    boolean isTest();
}
