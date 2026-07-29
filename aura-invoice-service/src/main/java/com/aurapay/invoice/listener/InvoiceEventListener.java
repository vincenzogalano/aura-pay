package com.aurapay.invoice.listener;

import com.aurapay.core.events.EventType;
import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.events.RefundSucceededEvent;
import com.aurapay.invoice.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventListener {

    private final InvoiceService invoiceService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = EventType.Topics.PAYMENT_SUCCEEDED, groupId = "invoice-service-group")
    public void onPaymentSucceeded(String rawMessage) {
        log.info("Kafka consumer received raw message on topic {}", EventType.Topics.PAYMENT_SUCCEEDED);
        try {
            PaymentSucceededEvent event = objectMapper.readValue(rawMessage, PaymentSucceededEvent.class);
            log.info("Parsed PaymentSucceededEvent paymentIntentId={}", event.paymentIntentId());
            invoiceService.processPaymentSucceeded(event);
        } catch (Exception e) {
            log.error("Error processing PaymentSucceededEvent raw={}: {}", rawMessage, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = EventType.Topics.REFUND_SUCCEEDED, groupId = "invoice-service-group")
    public void onRefundSucceeded(String rawMessage) {
        log.info("Kafka consumer received raw message on topic {}", EventType.Topics.REFUND_SUCCEEDED);
        try {
            RefundSucceededEvent event = objectMapper.readValue(rawMessage, RefundSucceededEvent.class);
            log.info("Parsed RefundSucceededEvent refundId={}", event.refundId());
            invoiceService.processRefundSucceeded(event);
        } catch (Exception e) {
            log.error("Error processing RefundSucceededEvent raw={}: {}", rawMessage, e.getMessage(), e);
        }
    }
}
