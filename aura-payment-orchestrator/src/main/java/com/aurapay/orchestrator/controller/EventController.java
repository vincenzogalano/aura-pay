package com.aurapay.orchestrator.controller;

import com.aurapay.orchestrator.domain.ExternalEvent;
import com.aurapay.orchestrator.domain.OutboxEvent;
import com.aurapay.orchestrator.repository.ExternalEventRepository;
import com.aurapay.orchestrator.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final OutboxEventRepository outboxEventRepository;
    private final ExternalEventRepository externalEventRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllEvents() {
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        List<ExternalEvent> externalEvents = externalEventRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();

        for (OutboxEvent evt : outboxEvents) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", evt.getId().toString());
            record.put("topic", evt.getEventType());
            record.put("partition", 0);
            record.put("offset", 1);
            record.put("timestamp", evt.getCreatedAt().toString());
            record.put("producerService", "aura-payment-orchestrator");

            try {
                Map<String, Object> payloadMap = objectMapper.readValue(evt.getPayload(), new TypeReference<Map<String, Object>>() {});
                record.put("payload", payloadMap);
            } catch (Exception e) {
                record.put("payload", Map.of("raw", evt.getPayload()));
            }

            result.add(record);
        }

        for (ExternalEvent evt : externalEvents) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", evt.getId().toString());
            record.put("topic", evt.getEventType());
            record.put("partition", 0);
            record.put("offset", 1);
            record.put("timestamp", evt.getCreatedAt().toString());
            record.put("producerService", deriveProducerService(evt.getEventType()));

            try {
                Map<String, Object> payloadMap = objectMapper.readValue(evt.getPayload(), new TypeReference<Map<String, Object>>() {});
                record.put("payload", payloadMap);
            } catch (Exception e) {
                record.put("payload", Map.of("raw", evt.getPayload()));
            }

            result.add(record);
        }

        // Sort descending by timestamp
        result.sort((a, b) -> String.valueOf(b.get("timestamp")).compareTo(String.valueOf(a.get("timestamp"))));

        return ResponseEntity.ok(result);
    }

    private String deriveProducerService(String eventType) {
        if (eventType.contains("invoice")) return "aura-invoice-service";
        if (eventType.contains("ledger")) return "aura-ledger-service";
        if (eventType.contains("merchant") || eventType.contains("apikey")) return "aura-merchant-service";
        if (eventType.contains("webhook")) return "aura-webhook-service";
        return "external-service";
    }
}
