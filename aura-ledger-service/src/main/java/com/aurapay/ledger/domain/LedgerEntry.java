package com.aurapay.ledger.domain;

import com.aurapay.ledger.domain.enums.AccountType;
import com.aurapay.ledger.domain.enums.EntryType;
import com.aurapay.ledger.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "entry_id", nullable = false, unique = true)
    private String entryId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "is_test", nullable = false)
    private boolean isTest;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
