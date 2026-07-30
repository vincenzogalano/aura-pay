package com.aurapay.ledger.consumer;

import com.aurapay.core.events.EventType;
import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.ledger.repository.ProcessedEventRepository;
import com.aurapay.ledger.service.LedgerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LedgerKafkaConsumerTest {

    @Mock
    private LedgerService ledgerService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private LedgerKafkaConsumer ledgerKafkaConsumer;

    private PaymentSucceededEvent paymentEvent;
    private String paymentJson;

    @BeforeEach
    void setUp() throws Exception {
        paymentEvent = new PaymentSucceededEvent(
                "evt_unique_123",
                EventType.PAYMENT_SUCCEEDED.getTopicName(),
                Instant.now(),
                "pi_test_001",
                "mch_001",
                5000L,
                150L,
                "EUR",
                "1111",
                "AUTH_99",
                "test@customer.it",
                "Test Payment",
                true
        );
        paymentJson = objectMapper.writeValueAsString(paymentEvent);
    }

    @Test
    @DisplayName("Should process new event and delegate to LedgerService")
    void consumeEvent_NewEvent_ShouldProcessSuccessfully() {

        given(processedEventRepository.existsById("evt_unique_123")).willReturn(false);


        ledgerKafkaConsumer.consumeEvent(paymentJson);


        verify(processedEventRepository).save(any());
        verify(ledgerService).recordPayment(any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("Should skip processing duplicate event to enforce effectively-once idempotency")
    void consumeEvent_DuplicateEvent_ShouldSkipProcessing() {

        given(processedEventRepository.existsById("evt_unique_123")).willReturn(true);


        ledgerKafkaConsumer.consumeEvent(paymentJson);


        verify(processedEventRepository, never()).save(any());
        verify(ledgerService, never()).recordPayment(any());
        verify(ledgerService, never()).recordRefund(any());
    }
}
