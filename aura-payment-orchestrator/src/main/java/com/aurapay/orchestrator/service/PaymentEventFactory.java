package com.aurapay.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aurapay.core.enums.AggregateType;
import com.aurapay.core.enums.PaymentFailureCode;
import com.aurapay.core.events.DomainEvent;
import com.aurapay.core.events.EventType;
import com.aurapay.core.events.PaymentFailedEvent;
import com.aurapay.core.events.PaymentIntentCreatedEvent;
import com.aurapay.core.events.PaymentProcessingEvent;
import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.events.RefundFailedEvent;
import com.aurapay.core.events.RefundRequestedEvent;
import com.aurapay.core.events.RefundSucceededEvent;
import com.aurapay.core.exception.AuraException;
import com.aurapay.orchestrator.domain.OutboxEvent;
import com.aurapay.orchestrator.domain.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventFactory {

    private final ObjectMapper objectMapper;

    public OutboxEvent buildCreatedOutboxEvent(PaymentIntent intent) {
        String eventId = UUID.randomUUID().toString();
        PaymentIntentCreatedEvent event = new PaymentIntentCreatedEvent(
                eventId,
                EventType.PAYMENT_INTENT_CREATED.getTopicName(),
                Instant.now(),
                intent.getId().toString(),
                intent.getMerchantId().toString(),
                intent.getAmountCents(),
                intent.getCurrency(),
                null,
                intent.isTest()
        );
        return createOutboxEvent(intent.getId().toString(), event.getEventType(), event);
    }

    public OutboxEvent buildProcessingOutboxEvent(PaymentIntent intent) {
        String eventId = UUID.randomUUID().toString();
        PaymentProcessingEvent event = new PaymentProcessingEvent(
                eventId,
                EventType.PAYMENT_PROCESSING.getTopicName(),
                Instant.now(),
                intent.getId().toString(),
                intent.getMerchantId().toString(),
                intent.getAmountCents(),
                intent.getCurrency(),
                intent.isTest()
        );
        return createOutboxEvent(intent.getId().toString(), event.getEventType(), event);
    }

    public OutboxEvent buildSucceededOutboxEvent(PaymentIntent intent, String cardLastFour) {
        String eventId = UUID.randomUUID().toString();
        long feeCents = (intent.getAmountCents() * 15) / 1000;
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                eventId,
                EventType.PAYMENT_SUCCEEDED.getTopicName(),
                Instant.now(),
                intent.getId().toString(),
                intent.getMerchantId().toString(),
                intent.getAmountCents(),
                feeCents,
                intent.getCurrency(),
                cardLastFour != null ? cardLastFour : "****",
                intent.getAuthorizationCode(),
                intent.getCustomerEmail(),
                intent.getDescription(),
                intent.isTest()
        );
        return createOutboxEvent(intent.getId().toString(), event.getEventType(), event);
    }

    public OutboxEvent buildFailedOutboxEvent(PaymentIntent intent, PaymentFailureCode failureCode, String failureMessage) {
        String eventId = UUID.randomUUID().toString();
        String codeStr = failureCode != null ? failureCode.name() : PaymentFailureCode.BANK_DECLINED.name();
        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                EventType.PAYMENT_FAILED.getTopicName(),
                Instant.now(),
                intent.getId().toString(),
                intent.getMerchantId().toString(),
                intent.getAmountCents(),
                intent.getCurrency(),
                codeStr,
                failureMessage != null ? failureMessage : intent.getFailureReason(),
                intent.isTest()
        );
        return createOutboxEvent(intent.getId().toString(), event.getEventType(), event);
    }

    public OutboxEvent buildRefundRequestedOutboxEvent(PaymentIntent intent, String refundId, long refundAmountCents, String reason) {
        String eventId = UUID.randomUUID().toString();
        RefundRequestedEvent event = new RefundRequestedEvent(
                eventId,
                EventType.REFUND_REQUESTED.getTopicName(),
                Instant.now(),
                refundId,
                intent.getId().toString(),
                intent.getMerchantId().toString(),
                refundAmountCents,
                reason != null ? reason : "Merchant requested refund",
                intent.isTest()
        );
        return createOutboxEvent(intent.getId().toString(), event.getEventType(), event);
    }

    public OutboxEvent buildRefundSucceededOutboxEvent(PaymentIntent intent, String refundId, long refundAmountCents, String reason) {
        String eventId = UUID.randomUUID().toString();
        RefundSucceededEvent event = new RefundSucceededEvent(
                eventId,
                EventType.REFUND_SUCCEEDED.getTopicName(),
                Instant.now(),
                refundId,
                intent.getId().toString(),
                intent.getMerchantId().toString(),
                refundAmountCents,
                reason != null ? reason : "Merchant requested refund",
                intent.isTest()
        );
        return createOutboxEvent(intent.getId().toString(), event.getEventType(), event);
    }

    public OutboxEvent buildRefundFailedOutboxEvent(PaymentIntent intent, String refundId, long refundAmountCents, String failureReason) {
        String eventId = UUID.randomUUID().toString();
        RefundFailedEvent event = new RefundFailedEvent(
                eventId,
                EventType.REFUND_FAILED.getTopicName(),
                Instant.now(),
                refundId,
                intent.getId().toString(),
                intent.getMerchantId().toString(),
                refundAmountCents,
                failureReason != null ? failureReason : "Bank declined refund",
                intent.isTest()
        );
        return createOutboxEvent(intent.getId().toString(), event.getEventType(), event);
    }

    private OutboxEvent createOutboxEvent(String aggregateId, String eventType, DomainEvent domainEvent) {
        try {
            String payloadJson = objectMapper.writeValueAsString(domainEvent);
            return OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType(AggregateType.PAYMENT_INTENT.getValue())
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .createdAt(Instant.now())
                    .processed(false)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event: {}", domainEvent, e);
            throw new AuraException("Failed to serialize outbox domain event: " + e.getMessage(), e);
        }
    }
}
