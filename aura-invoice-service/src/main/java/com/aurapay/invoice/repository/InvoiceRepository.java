package com.aurapay.invoice.repository;

import com.aurapay.invoice.domain.Invoice;
import com.aurapay.invoice.domain.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByPaymentIntentIdAndInvoiceType(UUID paymentIntentId, InvoiceType invoiceType);

    Optional<Invoice> findByRefundIdAndInvoiceType(UUID refundId, InvoiceType invoiceType);

    Page<Invoice> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    List<Invoice> findByIsTestOrderByCreatedAtDesc(boolean isTest);
    List<Invoice> findByMerchantIdAndIsTestOrderByCreatedAtDesc(UUID merchantId, boolean isTest);
    List<Invoice> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.merchantId = :merchantId AND i.invoiceType = :invoiceType AND YEAR(i.createdAt) = :year")
    long countByMerchantIdAndInvoiceTypeAndYear(@Param("merchantId") UUID merchantId,
                                               @Param("invoiceType") InvoiceType invoiceType,
                                               @Param("year") int year);
}
