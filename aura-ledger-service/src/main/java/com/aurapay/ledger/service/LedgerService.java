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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerEventPublisher ledgerEventPublisher;

    @Transactional
    public void recordPayment(PaymentSucceededEvent event) {
        log.info("Recording double-entry bookkeeping for payment: {} (merchant: {})",
                event.paymentIntentId(), event.merchantId());

        long grossAmount = event.amountCents();
        long feeAmount = event.feeCents();
        long netAmount = grossAmount - feeAmount;
        String currency = (event.currency() != null && !event.currency().isBlank()) ? event.currency() : "EUR";
        Instant now = Instant.now();

        String entryIdHolding = "led_hld_" + UUID.randomUUID().toString().substring(0, 8);
        LedgerEntry debitHolding = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .entryId(entryIdHolding)
                .merchantId(event.merchantId())
                .transactionType(TransactionType.PAYMENT)
                .entryType(EntryType.DEBIT)
                .accountType(AccountType.SETTLEMENT_HOLDING)
                .referenceId(event.paymentIntentId())
                .amountCents(grossAmount)
                .currency(currency)
                .isTest(event.isTest())
                .createdAt(now)
                .build();

        String entryIdMerchant = "led_mch_" + UUID.randomUUID().toString().substring(0, 8);
        LedgerEntry creditMerchant = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .entryId(entryIdMerchant)
                .merchantId(event.merchantId())
                .transactionType(TransactionType.PAYMENT)
                .entryType(EntryType.CREDIT)
                .accountType(AccountType.MERCHANT_AVAILABLE)
                .referenceId(event.paymentIntentId())
                .amountCents(netAmount)
                .currency(currency)
                .isTest(event.isTest())
                .createdAt(now)
                .build();

        String entryIdFee = "led_fee_" + UUID.randomUUID().toString().substring(0, 8);
        LedgerEntry creditFee = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .entryId(entryIdFee)
                .merchantId(event.merchantId())
                .transactionType(TransactionType.PAYMENT)
                .entryType(EntryType.CREDIT)
                .accountType(AccountType.SYSTEM_REVENUE)
                .referenceId(event.paymentIntentId())
                .amountCents(feeAmount)
                .currency(currency)
                .isTest(event.isTest())
                .createdAt(now)
                .build();

        ledgerEntryRepository.saveAll(List.of(debitHolding, creditMerchant, creditFee));

        ledgerEventPublisher.publishLedgerEntryRecorded(
                creditMerchant.getEntryId(),
                event.merchantId(),
                "PAYMENT",
                event.paymentIntentId(),
                grossAmount,
                AccountType.SETTLEMENT_HOLDING.name(),
                AccountType.MERCHANT_AVAILABLE.name(),
                event.isTest()
        );

        log.info("Double-entry bookkeeping recorded for payment {}: Debits={} Credits={} (Net={}, Fee={})",
                event.paymentIntentId(), grossAmount, netAmount + feeAmount, netAmount, feeAmount);
    }

    @Transactional
    public void recordRefund(RefundSucceededEvent event) {
        log.info("Recording double-entry bookkeeping for refund: {} (merchant: {})",
                event.refundId(), event.merchantId());

        long refundAmount = event.amountCents();
        Instant now = Instant.now();

        String entryIdDebit = "led_ref_deb_" + UUID.randomUUID().toString().substring(0, 8);
        LedgerEntry debitMerchant = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .entryId(entryIdDebit)
                .merchantId(event.merchantId())
                .transactionType(TransactionType.REFUND)
                .entryType(EntryType.DEBIT)
                .accountType(AccountType.MERCHANT_AVAILABLE)
                .referenceId(event.refundId())
                .amountCents(refundAmount)
                .currency("EUR")
                .isTest(event.isTest())
                .createdAt(now)
                .build();

        String entryIdCredit = "led_ref_crd_" + UUID.randomUUID().toString().substring(0, 8);
        LedgerEntry creditHolding = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .entryId(entryIdCredit)
                .merchantId(event.merchantId())
                .transactionType(TransactionType.REFUND)
                .entryType(EntryType.CREDIT)
                .accountType(AccountType.SETTLEMENT_HOLDING)
                .referenceId(event.refundId())
                .amountCents(refundAmount)
                .currency("EUR")
                .isTest(event.isTest())
                .createdAt(now)
                .build();

        ledgerEntryRepository.saveAll(List.of(debitMerchant, creditHolding));

        ledgerEventPublisher.publishLedgerEntryRecorded(
                debitMerchant.getEntryId(),
                event.merchantId(),
                "REFUND",
                event.refundId(),
                refundAmount,
                AccountType.MERCHANT_AVAILABLE.name(),
                AccountType.SETTLEMENT_HOLDING.name(),
                event.isTest()
        );

        log.info("Double-entry bookkeeping recorded for refund {}: Debits={} Credits={}",
                event.refundId(), refundAmount, refundAmount);
    }

    @Transactional(readOnly = true)
    public MerchantBalanceResponse getMerchantBalance(String merchantId, boolean isTest) {
        long balance = ledgerEntryRepository.calculateMerchantBalance(merchantId, isTest);
        return new MerchantBalanceResponse(
                merchantId,
                balance,
                "EUR",
                isTest,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getMerchantEntries(String merchantId, boolean isTest, Pageable pageable) {
        return ledgerEntryRepository.findByMerchantIdAndIsTestOrderByCreatedAtDesc(merchantId, isTest, pageable)
                .map(LedgerEntryResponse::fromEntity);
    }
}
