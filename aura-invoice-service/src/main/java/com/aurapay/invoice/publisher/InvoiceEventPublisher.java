package com.aurapay.invoice.publisher;

import com.aurapay.core.events.EventType;
import com.aurapay.core.events.InvoiceGeneratedEvent;
import com.aurapay.core.events.InvoiceGenerationFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishInvoiceGenerated(InvoiceGeneratedEvent event) {
        String topic = EventType.INVOICE_GENERATED.getTopicName();
        String partitionKey = event.merchantId() != null ? event.merchantId() : event.eventId();
        log.info("Publishing InvoiceGeneratedEvent to topic={} key={} invoiceId={}", topic, partitionKey, event.invoiceId());
        kafkaTemplate.send(topic, partitionKey, event);
    }

    public void publishInvoiceGenerationFailed(InvoiceGenerationFailedEvent event) {
        String topic = EventType.INVOICE_GENERATION_FAILED.getTopicName();
        String partitionKey = event.merchantId() != null ? event.merchantId() : event.eventId();
        log.info("Publishing InvoiceGenerationFailedEvent to topic={} key={} paymentIntentId={}", topic, partitionKey, event.paymentIntentId());
        kafkaTemplate.send(topic, partitionKey, event);
    }
}
