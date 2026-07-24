package com.aurapay.ledger.dto.response;

import com.aurapay.ledger.domain.LedgerEntry;

import java.time.Instant;

public record LedgerEntryResponse(
        String entryId,
        String merchantId,
        String transactionType,
        String entryType,
        String accountType,
        String referenceId,
        long amountCents,
        String currency,
        boolean isTest,
        Instant createdAt
) {
    public static LedgerEntryResponse fromEntity(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getEntryId(),
                entry.getMerchantId(),
                entry.getTransactionType().name(),
                entry.getEntryType().name(),
                entry.getAccountType().name(),
                entry.getReferenceId(),
                entry.getAmountCents(),
                entry.getCurrency(),
                entry.isTest(),
                entry.getCreatedAt()
        );
    }
}
