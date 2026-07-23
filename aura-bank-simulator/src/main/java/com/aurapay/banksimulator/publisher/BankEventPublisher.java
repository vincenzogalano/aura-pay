package com.aurapay.banksimulator.publisher;

import com.aurapay.core.events.BankAuthorizationResultEvent;
import com.aurapay.core.events.EventType;
import com.aurapay.banksimulator.dto.request.BankAuthorizationRequest;
import com.aurapay.banksimulator.dto.response.BankAuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class BankEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BankEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BankEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAuthorizationResult(BankAuthorizationRequest request, BankAuthorizationResponse response) {
        try {
            BankAuthorizationResultEvent event = new BankAuthorizationResultEvent(
                    "evt_" + UUID.randomUUID().toString().substring(0, 8),
                    EventType.BANK_AUTHORIZATION_RESULT.getTopicName(),
                    Instant.now(),
                    response.transactionId(),
                    request.paymentIntentId().toString(),
                    response.authorized(),
                    response.responseCode(),
                    response.authorizationCode(),
                    response.declineReason(),
                    request.isTest()
            );

            kafkaTemplate.send(EventType.BANK_AUTHORIZATION_RESULT.getTopicName(), request.merchantId().toString(), event);
            log.info("Published BankAuthorizationResultEvent to Kafka for PaymentIntent: {}", request.paymentIntentId());
        } catch (Exception e) {
            log.warn("Failed to publish BankAuthorizationResultEvent to Kafka: {}", e.getMessage());
        }
    }
}
