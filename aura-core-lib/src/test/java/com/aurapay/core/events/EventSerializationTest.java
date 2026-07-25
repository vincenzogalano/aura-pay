package com.aurapay.core.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("Should serialize and deserialize PaymentSucceededEvent record flawlessly")
    void shouldSerializePaymentSucceededEvent() throws Exception {
        PaymentSucceededEvent originalEvent = new PaymentSucceededEvent(
                "evt_" + UUID.randomUUID(),
                null,
                Instant.now(),
                "pi_99887766",
                "mch_12345",
                25000L,
                750L,
                "EUR",
                "1111",
                "AUTH_CODE_888",
                true
        );

        String json = objectMapper.writeValueAsString(originalEvent);

        assertThat(json)
                .contains("aura.payment.succeeded.v1")
                .contains("pi_99887766")
                .contains("mch_12345");

        PaymentSucceededEvent deserializedEvent = objectMapper.readValue(json, PaymentSucceededEvent.class);

        assertThat(deserializedEvent)
                .isNotNull()
                .isEqualTo(originalEvent);

        assertThat(deserializedEvent.getEventType()).isEqualTo("aura.payment.succeeded.v1");
        assertThat(deserializedEvent.getEventTypeEnum()).isEqualTo(EventType.PAYMENT_SUCCEEDED);
        assertThat(deserializedEvent.isTest()).isTrue();
    }

    @Test
    @DisplayName("Should serialize and deserialize MerchantCreatedEvent record flawlessly")
    void shouldSerializeMerchantCreatedEvent() throws Exception {
        MerchantCreatedEvent originalEvent = new MerchantCreatedEvent(
                "evt_" + UUID.randomUUID(),
                null,
                Instant.now(),
                "mch_443322",
                "Acme Corp",
                "IT12345678901",
                "admin@acme.com",
                false
        );

        String json = objectMapper.writeValueAsString(originalEvent);
        MerchantCreatedEvent deserialized = objectMapper.readValue(json, MerchantCreatedEvent.class);

        assertThat(deserialized).isEqualTo(originalEvent);
        assertThat(deserialized.getEventType()).isEqualTo("aura.merchant.created.v1");
    }
}
