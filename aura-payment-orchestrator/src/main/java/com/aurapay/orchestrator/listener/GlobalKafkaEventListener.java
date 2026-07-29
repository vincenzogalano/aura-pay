package com.aurapay.orchestrator.listener;

import com.aurapay.core.events.EventType;
import com.aurapay.orchestrator.domain.ExternalEvent;
import com.aurapay.orchestrator.repository.ExternalEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalKafkaEventListener {

    private final ExternalEventRepository externalEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                    EventType.Topics.MERCHANT_CREATED,
                    EventType.Topics.MERCHANT_VERIFIED,
                    EventType.Topics.MERCHANT_VERIFICATION_REJECTED,
                    EventType.Topics.API_KEY_CREATED,
                    EventType.Topics.API_KEY_REVOKED,
                    EventType.Topics.INVOICE_GENERATED,
                    EventType.Topics.INVOICE_GENERATION_FAILED,
                    EventType.Topics.LEDGER_ENTRY_RECORDED,
                    EventType.Topics.WEBHOOK_DELIVERY_SUCCEEDED,
                    EventType.Topics.WEBHOOK_DELIVERY_DEAD_LETTERED
            },
            groupId = "orchestrator-event-collector-group"
    )
    public void onExternalDomainEvent(
            String rawMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        try {
            JsonNode node = objectMapper.readTree(rawMessage);
            String aggregateId = extractAggregateId(node);
            String aggIdStr = aggregateId != null ? aggregateId : topic;

            // Idempotency check: don't duplicate identical event for same aggregateId
            if (externalEventRepository.existsByEventTypeAndAggregateId(topic, aggIdStr)) {
                return;
            }

            ExternalEvent externalEvent = ExternalEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("DOMAIN_EVENT")
                    .aggregateId(aggIdStr)
                    .eventType(topic)
                    .payload(rawMessage)
                    .createdAt(Instant.now())
                    .build();

            externalEventRepository.save(externalEvent);
            log.info("Saved external domain event to external_events store topic={} aggregateId={}", topic, aggIdStr);

        } catch (Exception e) {
            log.error("Failed to store cross-service event topic={}: {}", topic, e.getMessage(), e);
        }
    }

    private String extractAggregateId(JsonNode node) {
        if (node.has("eventId")) return node.get("eventId").asText();
        if (node.has("invoiceId")) return node.get("invoiceId").asText();
        if (node.has("merchantId")) return node.get("merchantId").asText();
        if (node.has("apiKeyId")) return node.get("apiKeyId").asText();
        if (node.has("paymentIntentId")) return node.get("paymentIntentId").asText();
        return null;
    }
}
