package com.aurapay.merchant.publisher;

import com.aurapay.core.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMerchantCreated(String merchantId, String businessName, String vatNumber, String email) {
        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
        MerchantCreatedEvent event = new MerchantCreatedEvent(
                eventId,
                EventType.MERCHANT_CREATED.getTopicName(),
                Instant.now(),
                merchantId,
                businessName,
                vatNumber,
                email,
                true
        );
        log.info("Publishing MerchantCreatedEvent to topic {}: {}", EventType.MERCHANT_CREATED.getTopicName(), event);
        kafkaTemplate.send(EventType.MERCHANT_CREATED.getTopicName(), merchantId, event);
    }

    public void publishMerchantVerified(String merchantId, String businessName, String vatNumber) {
        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
        MerchantVerifiedEvent event = new MerchantVerifiedEvent(
                eventId,
                EventType.MERCHANT_VERIFIED.getTopicName(),
                Instant.now(),
                merchantId,
                businessName,
                vatNumber,
                false
        );
        log.info("Publishing MerchantVerifiedEvent to topic {}: {}", EventType.MERCHANT_VERIFIED.getTopicName(), event);
        kafkaTemplate.send(EventType.MERCHANT_VERIFIED.getTopicName(), merchantId, event);
    }

    public void publishMerchantVerificationRejected(String merchantId, String reason) {
        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
        MerchantVerificationRejectedEvent event = new MerchantVerificationRejectedEvent(
                eventId,
                EventType.MERCHANT_VERIFICATION_REJECTED.getTopicName(),
                Instant.now(),
                merchantId,
                reason,
                false
        );
        log.info("Publishing MerchantVerificationRejectedEvent to topic {}: {}", EventType.MERCHANT_VERIFICATION_REJECTED.getTopicName(), event);
        kafkaTemplate.send(EventType.MERCHANT_VERIFICATION_REJECTED.getTopicName(), merchantId, event);
    }

    public void publishApiKeyCreated(String apiKeyId, String merchantId, String keyPrefix, String environment, boolean isTest) {
        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
        ApiKeyCreatedEvent event = new ApiKeyCreatedEvent(
                eventId,
                EventType.API_KEY_CREATED.getTopicName(),
                Instant.now(),
                apiKeyId,
                merchantId,
                keyPrefix,
                environment,
                isTest
        );
        log.info("Publishing ApiKeyCreatedEvent to topic {}: {}", EventType.API_KEY_CREATED.getTopicName(), event);
        kafkaTemplate.send(EventType.API_KEY_CREATED.getTopicName(), merchantId, event);
    }

    public void publishApiKeyRevoked(String apiKeyId, String merchantId, String keyPrefix, boolean isTest) {
        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
        ApiKeyRevokedEvent event = new ApiKeyRevokedEvent(
                eventId,
                EventType.API_KEY_REVOKED.getTopicName(),
                Instant.now(),
                apiKeyId,
                merchantId,
                keyPrefix,
                isTest
        );
        log.info("Publishing ApiKeyRevokedEvent to topic {}: {}", EventType.API_KEY_REVOKED.getTopicName(), event);
        kafkaTemplate.send(EventType.API_KEY_REVOKED.getTopicName(), merchantId, event);
    }
}
