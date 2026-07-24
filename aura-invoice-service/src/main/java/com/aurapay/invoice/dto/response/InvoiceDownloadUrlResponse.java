package com.aurapay.invoice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InvoiceDownloadUrlResponse(
        UUID invoiceId,
        String downloadUrl,
        Instant expiresAt
) {
}
