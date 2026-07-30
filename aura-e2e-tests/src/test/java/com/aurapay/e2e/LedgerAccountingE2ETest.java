package com.aurapay.e2e;

import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.events.RefundSucceededEvent;
import com.aurapay.ledger.dto.response.MerchantBalanceResponse;
import com.aurapay.ledger.service.LedgerEventPublisher;
import com.aurapay.ledger.service.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ledger_e2edb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false"
})
@Transactional
class LedgerAccountingE2ETest {

    @Autowired
    private LedgerService ledgerService;

    @MockitoBean
    private LedgerEventPublisher ledgerEventPublisher;

    @Test
    @DisplayName("E2E - Registrazione contabile in Partita Doppia per pagamento e calcolo saldo algebrico net (Gross - Fee)")
    void e2e_ledgerRecordPayment_calculatesNetBalance() {
        UUID merchantId = UUID.randomUUID();
        UUID paymentIntentId = UUID.randomUUID();

        // Gross = 100.00 EUR (10000 cents), Fee = 2.50 EUR (250 cents) -> Net = 97.50 EUR (9750 cents)
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                "evt_1001",
                "payment.succeeded",
                Instant.now(),
                paymentIntentId.toString(),
                merchantId.toString(),
                10000L,
                250L,
                "EUR",
                "1111",
                "AUTH_1001",
                true
        );

        ledgerService.recordPayment(event);

        MerchantBalanceResponse balanceResponse = ledgerService.getMerchantBalance(merchantId.toString(), true);

        assertThat(balanceResponse).isNotNull();
        assertThat(balanceResponse.merchantId()).isEqualTo(merchantId.toString());
        assertThat(balanceResponse.availableBalanceCents()).isEqualTo(9750L); // 10000 - 250
        assertThat(balanceResponse.currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("E2E - Registrazione contabile per rimborso aggiorna correttamente il saldo netto del merchant")
    void e2e_ledgerRecordRefund_decreasesMerchantBalance() {
        UUID merchantId = UUID.randomUUID();
        UUID paymentIntentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();

        // 1. Payment: Gross = 10000 cents, Fee = 300 cents -> Net = 9700 cents
        PaymentSucceededEvent payEvent = new PaymentSucceededEvent(
                "evt_1002",
                "payment.succeeded",
                Instant.now(),
                paymentIntentId.toString(),
                merchantId.toString(),
                10000L,
                300L,
                "EUR",
                "1111",
                "AUTH_1002",
                true
        );
        ledgerService.recordPayment(payEvent);

        // 2. Refund: 2000 cents refunded
        RefundSucceededEvent refundEvent = new RefundSucceededEvent(
                "evt_1003",
                "refund.succeeded",
                Instant.now(),
                refundId.toString(),
                paymentIntentId.toString(),
                merchantId.toString(),
                2000L,
                "requested_by_customer",
                true
        );
        ledgerService.recordRefund(refundEvent);

        MerchantBalanceResponse balance = ledgerService.getMerchantBalance(merchantId.toString(), true);

        assertThat(balance.availableBalanceCents()).isEqualTo(7700L); // 9700 - 2000
    }

    @Test
    @DisplayName("E2E - Isolamento rigido dei saldi tra ambiente Sandbox (isTest=true) e Live (isTest=false)")
    void e2e_ledgerSandboxLiveIsolation() {
        UUID merchantId = UUID.randomUUID();

        // Sandbox Payment: 5000 cents gross, 100 fee
        PaymentSucceededEvent sandboxPay = new PaymentSucceededEvent(
                "evt_sb", "payment.succeeded", Instant.now(), UUID.randomUUID().toString(),
                merchantId.toString(), 5000L, 100L, "EUR", "1111", "AUTH_SB", true
        );
        ledgerService.recordPayment(sandboxPay);

        // Live Payment: 20000 cents gross, 500 fee
        PaymentSucceededEvent livePay = new PaymentSucceededEvent(
                "evt_live", "payment.succeeded", Instant.now(), UUID.randomUUID().toString(),
                merchantId.toString(), 20000L, 500L, "EUR", "1111", "AUTH_LIVE", false
        );
        ledgerService.recordPayment(livePay);

        MerchantBalanceResponse sandboxBal = ledgerService.getMerchantBalance(merchantId.toString(), true);
        MerchantBalanceResponse liveBal = ledgerService.getMerchantBalance(merchantId.toString(), false);

        assertThat(sandboxBal.availableBalanceCents()).isEqualTo(4900L); // 5000 - 100
        assertThat(liveBal.availableBalanceCents()).isEqualTo(19500L); // 20000 - 500
    }
}
