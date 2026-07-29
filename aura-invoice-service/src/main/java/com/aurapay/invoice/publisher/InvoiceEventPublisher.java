package com.aurapay.invoice.publisher;

import com.aurapay.core.events.EventType;
import com.aurapay.core.events.InvoiceGeneratedEvent;
import com.aurapay.core.events.InvoiceGenerationFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishInvoiceGenerated(InvoiceGeneratedEvent event) {
        String topic = EventType.INVOICE_GENERATED.getTopicName();
        String partitionKey = event.merchantId() != null ? event.merchantId() : event.eventId();
        log.info("Publishing InvoiceGeneratedEvent to topic={} key={} invoiceId={}", topic, partitionKey, event.invoiceId());
        try {
            kafkaTemplate.send(topic, partitionKey, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish InvoiceGeneratedEvent: {}", e.getMessage(), e);
        }
    }

    public void publishInvoiceGenerationFailed(InvoiceGenerationFailedEvent event) {
        String topic = EventType.INVOICE_GENERATION_FAILED.getTopicName();
        String partitionKey = event.merchantId() != null ? event.merchantId() : event.eventId();
        log.info("Publishing InvoiceGenerationFailedEvent to topic={} key={} paymentIntentId={}", topic, partitionKey, event.paymentIntentId());
        try {
            kafkaTemplate.send(topic, partitionKey, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish InvoiceGenerationFailedEvent: {}", e.getMessage(), e);
        }
    }
}
