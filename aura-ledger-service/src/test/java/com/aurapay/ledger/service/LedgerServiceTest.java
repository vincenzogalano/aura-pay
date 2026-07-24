package com.aurapay.ledger.service;

import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.events.RefundSucceededEvent;
import com.aurapay.ledger.domain.LedgerEntry;
import com.aurapay.ledger.domain.enums.AccountType;
import com.aurapay.ledger.domain.enums.EntryType;
import com.aurapay.ledger.domain.enums.TransactionType;
import com.aurapay.ledger.dto.response.LedgerEntryResponse;
import com.aurapay.ledger.dto.response.MerchantBalanceResponse;
import com.aurapay.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private LedgerEventPublisher ledgerEventPublisher;

    @InjectMocks
    private LedgerService ledgerService;

    private PaymentSucceededEvent paymentEvent;
    private RefundSucceededEvent refundEvent;

    @BeforeEach
    void setUp() {
        paymentEvent = new PaymentSucceededEvent(
                "evt_100",
                "aura.payment.succeeded.v1",
                Instant.now(),
                "pi_test_123",
                "mch_test_99",
                10000L, // 100.00 EUR gross
                300L,   // 3.00 EUR fee
                "EUR",
                "4242",
                "AUTH_123",
                true
        );

        refundEvent = new RefundSucceededEvent(
                "evt_200",
                "aura.refund.succeeded.v1",
                Instant.now(),
                "re_test_456",
                "pi_test_123",
                "mch_test_99",
                2000L, // 20.00 EUR refund
                "customer_request",
                true
        );
    }

    @Test
    @DisplayName("Should record payment entries with strictly balanced double-entry accounting (Debits == Credits)")
    void recordPayment_ShouldCreateBalancedDoubleEntry() {
        // Act
        ledgerService.recordPayment(paymentEvent);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());

        List<LedgerEntry> savedEntries = captor.getValue();
        assertThat(savedEntries).hasSize(3);

        long totalDebits = savedEntries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .mapToLong(LedgerEntry::getAmountCents)
                .sum();

        long totalCredits = savedEntries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .mapToLong(LedgerEntry::getAmountCents)
                .sum();

        assertThat(totalDebits).isEqualTo(10000L);
        assertThat(totalCredits).isEqualTo(10000L);
        assertThat(totalDebits).isEqualTo(totalCredits);

        LedgerEntry merchantEntry = savedEntries.stream()
                .filter(e -> e.getAccountType() == AccountType.MERCHANT_AVAILABLE)
                .findFirst()
                .orElseThrow();

        assertThat(merchantEntry.getAmountCents()).isEqualTo(9700L); // 10000 - 300
        assertThat(merchantEntry.getEntryType()).isEqualTo(EntryType.CREDIT);

        LedgerEntry feeEntry = savedEntries.stream()
                .filter(e -> e.getAccountType() == AccountType.SYSTEM_REVENUE)
                .findFirst()
                .orElseThrow();

        assertThat(feeEntry.getAmountCents()).isEqualTo(300L);
        assertThat(feeEntry.getEntryType()).isEqualTo(EntryType.CREDIT);

        verify(ledgerEventPublisher).publishLedgerEntryRecorded(
                any(), eq("mch_test_99"), eq("PAYMENT"), eq("pi_test_123"), eq(10000L), any(), any(), eq(true)
        );
    }

    @Test
    @DisplayName("Should record refund entries with balanced double-entry accounting")
    void recordRefund_ShouldCreateBalancedDoubleEntry() {
        // Act
        ledgerService.recordRefund(refundEvent);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());

        List<LedgerEntry> savedEntries = captor.getValue();
        assertThat(savedEntries).hasSize(2);

        long totalDebits = savedEntries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .mapToLong(LedgerEntry::getAmountCents)
                .sum();

        long totalCredits = savedEntries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .mapToLong(LedgerEntry::getAmountCents)
                .sum();

        assertThat(totalDebits).isEqualTo(2000L);
        assertThat(totalCredits).isEqualTo(2000L);
        assertThat(totalDebits).isEqualTo(totalCredits);

        LedgerEntry merchantEntry = savedEntries.stream()
                .filter(e -> e.getAccountType() == AccountType.MERCHANT_AVAILABLE)
                .findFirst()
                .orElseThrow();

        assertThat(merchantEntry.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(merchantEntry.getAmountCents()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("Should calculate merchant available balance algebraically")
    void getMerchantBalance_ShouldReturnAlgebraicBalance() {
        // Arrange
        given(ledgerEntryRepository.calculateMerchantBalance("mch_test_99", true))
                .willReturn(7700L);

        // Act
        MerchantBalanceResponse response = ledgerService.getMerchantBalance("mch_test_99", true);

        // Assert
        assertThat(response.merchantId()).isEqualTo("mch_test_99");
        assertThat(response.availableBalanceCents()).isEqualTo(7700L);
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.isTest()).isTrue();
    }

    @Test
    @DisplayName("Should return paginated ledger entry responses")
    void getMerchantEntries_ShouldReturnPaginatedList() {
        // Arrange
        LedgerEntry entry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .entryId("led_100")
                .merchantId("mch_test_99")
                .transactionType(TransactionType.PAYMENT)
                .entryType(EntryType.CREDIT)
                .accountType(AccountType.MERCHANT_AVAILABLE)
                .referenceId("pi_test_123")
                .amountCents(9700L)
                .currency("EUR")
                .isTest(true)
                .createdAt(Instant.now())
                .build();

        PageImpl<LedgerEntry> page = new PageImpl<>(List.of(entry));
        given(ledgerEntryRepository.findByMerchantIdAndIsTestOrderByCreatedAtDesc(eq("mch_test_99"), eq(true), any()))
                .willReturn(page);

        // Act
        Page<LedgerEntryResponse> result = ledgerService.getMerchantEntries("mch_test_99", true, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().entryId()).isEqualTo("led_100");
        assertThat(result.getContent().getFirst().amountCents()).isEqualTo(9700L);
    }
}
