package com.aurapay.banksimulator.service;

import com.aurapay.core.exception.BusinessException;
import com.aurapay.banksimulator.dto.request.BankAuthorizationRequest;
import com.aurapay.banksimulator.dto.response.BankAuthorizationResponse;
import com.aurapay.banksimulator.dto.request.BankRefundRequest;
import com.aurapay.banksimulator.dto.response.BankRefundResponse;
import com.aurapay.banksimulator.publisher.BankEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class BankSimulatorServiceTest {

    @Mock
    private BankEventPublisher eventPublisher;

    private BankSimulatorService service;

    @BeforeEach
    void setUp() {
        service = new BankSimulatorService(eventPublisher, 0L);
    }

    @Test
    @DisplayName("Should approve authorization for standard amounts")
    void authorize_approved() {
        BankAuthorizationRequest request = new BankAuthorizationRequest(
                UUID.randomUUID(), UUID.randomUUID(), 5000L, "EUR", "tok_test_123", true
        );

        BankAuthorizationResponse response = service.authorize(request);

        assertThat(response.authorized()).isTrue();
        assertThat(response.responseCode()).isEqualTo("00");
        assertThat(response.transactionId()).startsWith("tx_bank_");
        assertThat(response.authorizationCode()).startsWith("AUTH_");
        assertThat(response.declineReason()).isNull();
    }

    @Test
    @DisplayName("Should decline authorization with INSUFFICIENT_FUNDS when amount ends in 99")
    void authorize_insufficientFunds() {
        BankAuthorizationRequest request = new BankAuthorizationRequest(
                UUID.randomUUID(), UUID.randomUUID(), 1099L, "EUR", "tok_test_123", true
        );

        BankAuthorizationResponse response = service.authorize(request);

        assertThat(response.authorized()).isFalse();
        assertThat(response.responseCode()).isEqualTo("51");
        assertThat(response.declineReason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(response.transactionId()).isNull();
    }

    @Test
    @DisplayName("Should decline authorization with EXPIRED_CARD when amount ends in 98")
    void authorize_expiredCard() {
        BankAuthorizationRequest request = new BankAuthorizationRequest(
                UUID.randomUUID(), UUID.randomUUID(), 2098L, "EUR", "tok_test_123", true
        );

        BankAuthorizationResponse response = service.authorize(request);

        assertThat(response.authorized()).isFalse();
        assertThat(response.responseCode()).isEqualTo("54");
        assertThat(response.declineReason()).isEqualTo("EXPIRED_CARD");
    }

    @Test
    @DisplayName("Should decline authorization with SUSPECTED_FRAUD when amount ends in 97")
    void authorize_suspectedFraud() {
        BankAuthorizationRequest request = new BankAuthorizationRequest(
                UUID.randomUUID(), UUID.randomUUID(), 3097L, "EUR", "tok_test_123", true
        );

        BankAuthorizationResponse response = service.authorize(request);

        assertThat(response.authorized()).isFalse();
        assertThat(response.responseCode()).isEqualTo("59");
        assertThat(response.declineReason()).isEqualTo("SUSPECTED_FRAUD");
    }

    @Test
    @DisplayName("Should throw BusinessException BANK_UNAVAILABLE when amount ends in 95")
    void authorize_bankUnavailable() {
        BankAuthorizationRequest request = new BankAuthorizationRequest(
                UUID.randomUUID(), UUID.randomUUID(), 1095L, "EUR", "tok_test_123", true
        );

        assertThatThrownBy(() -> service.authorize(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("timeout simulated");
    }

    @Test
    @DisplayName("Should process successful refund")
    void refund_success() {
        BankRefundRequest request = new BankRefundRequest(
                "tx_bank_12345", UUID.randomUUID(), 2000L, "customer_request"
        );

        BankRefundResponse response = service.refund(request);

        assertThat(response.success()).isTrue();
        assertThat(response.responseCode()).isEqualTo("00");
        assertThat(response.refundTransactionId()).startsWith("tx_ref_");
    }

    @Test
    @DisplayName("Should fail refund when amount ends in 99")
    void refund_failed() {
        BankRefundRequest request = new BankRefundRequest(
                "tx_bank_12345", UUID.randomUUID(), 1999L, "customer_request"
        );

        BankRefundResponse response = service.refund(request);

        assertThat(response.success()).isFalse();
        assertThat(response.responseCode()).isEqualTo("51");
    }
}
