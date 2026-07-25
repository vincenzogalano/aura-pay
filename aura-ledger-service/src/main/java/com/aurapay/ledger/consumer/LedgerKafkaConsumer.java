package com.aurapay.ledger.consumer;

import com.aurapay.core.events.EventType;
import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.events.RefundSucceededEvent;
import com.aurapay.ledger.domain.ProcessedEvent;
import com.aurapay.ledger.repository.ProcessedEventRepository;
import com.aurapay.ledger.service.LedgerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerKafkaConsumer {

    private final LedgerService ledgerService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {EventType.Topics.PAYMENT_SUCCEEDED, EventType.Topics.REFUND_SUCCEEDED},
            groupId = "ledger-service-group"
    )
    @Transactional
    public void consumeEvent(String messagePayload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(messagePayload);
            String eventId = jsonNode.has("eventId") ? jsonNode.get("eventId").asText() : null;
            String eventTypeStr = jsonNode.has("eventType") ? jsonNode.get("eventType").asText() : null;

            if (eventId == null || eventId.isBlank()) {
                log.error("Received event with missing eventId payload: {}", messagePayload);
                return;
            }

            if (processedEventRepository.existsById(eventId)) {
                log.warn("Event {} already processed. Skipping duplicate processing for idempotency.", eventId);
                return;
            }

            // Save processed event for effectively-once semantics
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(eventTypeStr != null ? eventTypeStr : "UNKNOWN")
                    .processedAt(Instant.now())
                    .build();
            processedEventRepository.save(processedEvent);

            if (EventType.PAYMENT_SUCCEEDED.getTopicName().equals(eventTypeStr)) {
                PaymentSucceededEvent event = objectMapper.treeToValue(jsonNode, PaymentSucceededEvent.class);
                ledgerService.recordPayment(event);
            } else if (EventType.REFUND_SUCCEEDED.getTopicName().equals(eventTypeStr)) {
                RefundSucceededEvent event = objectMapper.treeToValue(jsonNode, RefundSucceededEvent.class);
                ledgerService.recordRefund(event);
            } else {
                log.warn("Unhandled eventType: {} for eventId: {}", eventTypeStr, eventId);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message in ledger-service-group: {}", messagePayload, e);
            throw new RuntimeException("Error processing Kafka message in LedgerKafkaConsumer", e);
        }
    }
}
