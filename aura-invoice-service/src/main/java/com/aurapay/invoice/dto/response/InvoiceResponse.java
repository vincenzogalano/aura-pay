package com.aurapay.invoice.dto.response;

import com.aurapay.invoice.domain.Invoice;
import com.aurapay.invoice.domain.enums.InvoiceStatus;
import com.aurapay.invoice.domain.enums.InvoiceType;

import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID merchantId,
        UUID paymentIntentId,
        UUID refundId,
        InvoiceType invoiceType,
        Long amountCents,
        String currency,
        String pdfObjectKey,
        InvoiceStatus status,
        Boolean isTest,
        Instant createdAt
) {
    public static InvoiceResponse fromEntity(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getMerchantId(),
                invoice.getPaymentIntentId(),
                invoice.getRefundId(),
                invoice.getInvoiceType(),
                invoice.getAmountCents(),
                invoice.getCurrency(),
                invoice.getPdfObjectKey(),
                invoice.getStatus(),
                invoice.getIsTest(),
                invoice.getCreatedAt()
        );
    }
}
