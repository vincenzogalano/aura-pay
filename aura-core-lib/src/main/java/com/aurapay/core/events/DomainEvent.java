package com.aurapay.core.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
public interface DomainEvent {
    String getEventId();
    String getEventType();
    @JsonIgnore
    default EventType getEventTypeEnum() {
        return EventType.fromTopicName(getEventType());
    }
    Instant getOccurredAt();
    boolean isTest();
}
