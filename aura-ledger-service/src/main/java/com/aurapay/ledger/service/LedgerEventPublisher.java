package com.aurapay.ledger.service;

import com.aurapay.core.events.EventType;
import com.aurapay.core.events.LedgerEntryRecordedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishLedgerEntryRecorded(
            String entryId,
            String merchantId,
            String referenceType,
            String referenceId,
            long amountCents,
            String debitAccount,
            String creditAccount,
            boolean isTest
    ) {
        String topicName = EventType.LEDGER_ENTRY_RECORDED.getTopicName();
        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);

        LedgerEntryRecordedEvent event = new LedgerEntryRecordedEvent(
                eventId,
                topicName,
                Instant.now(),
                entryId,
                merchantId,
                referenceType,
                referenceId,
                amountCents,
                debitAccount,
                creditAccount,
                isTest
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Publishing LedgerEntryRecordedEvent for entryId: {} on topic: {}", entryId, topicName);
            kafkaTemplate.send(topicName, merchantId, jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish LedgerEntryRecordedEvent for entryId: {}", entryId, e);
        }
    }
}
