package com.aurapay.ledger.repository;

import com.aurapay.ledger.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = com.aurapay.ledger.domain.enums.EntryType.CREDIT THEN e.amountCents ELSE -e.amountCents END), 0) " +
           "FROM LedgerEntry e " +
           "WHERE e.merchantId = :merchantId AND e.accountType = com.aurapay.ledger.domain.enums.AccountType.MERCHANT_AVAILABLE AND e.isTest = :isTest")
    long calculateMerchantBalance(@Param("merchantId") String merchantId, @Param("isTest") boolean isTest);

    Page<LedgerEntry> findByMerchantIdAndIsTestOrderByCreatedAtDesc(String merchantId, boolean isTest, Pageable pageable);

    List<LedgerEntry> findByReferenceId(String referenceId);
}
