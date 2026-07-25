package com.aurapay.invoice.listener;

import com.aurapay.core.events.EventType;
import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.events.RefundSucceededEvent;
import com.aurapay.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventListener {

    private final InvoiceService invoiceService;

    @KafkaListener(topics = EventType.Topics.PAYMENT_SUCCEEDED, groupId = "invoice-service-group")
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Kafka consumer received PaymentSucceededEvent paymentIntentId={}", event.paymentIntentId());
        try {
            invoiceService.processPaymentSucceeded(event);
        } catch (Exception e) {
            log.error("Error processing PaymentSucceededEvent paymentIntentId={}: {}", event.paymentIntentId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = EventType.Topics.REFUND_SUCCEEDED, groupId = "invoice-service-group")
    public void onRefundSucceeded(RefundSucceededEvent event) {
        log.info("Kafka consumer received RefundSucceededEvent refundId={}", event.refundId());
        try {
            invoiceService.processRefundSucceeded(event);
        } catch (Exception e) {
            log.error("Error processing RefundSucceededEvent refundId={}: {}", event.refundId(), e.getMessage(), e);
        }
    }
}
