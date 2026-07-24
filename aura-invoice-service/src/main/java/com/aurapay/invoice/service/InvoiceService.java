package com.aurapay.invoice.service;

import com.aurapay.core.events.InvoiceGeneratedEvent;
import com.aurapay.core.events.InvoiceGenerationFailedEvent;
import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.events.RefundSucceededEvent;
import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.exception.ResourceNotFoundException;
import com.aurapay.core.security.HmacUtils;
import com.aurapay.invoice.domain.Invoice;
import com.aurapay.invoice.domain.enums.InvoiceStatus;
import com.aurapay.invoice.domain.enums.InvoiceType;
import com.aurapay.invoice.dto.response.InvoiceDownloadUrlResponse;
import com.aurapay.invoice.dto.response.InvoiceResponse;
import com.aurapay.invoice.publisher.InvoiceEventPublisher;
import com.aurapay.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePdfGenerator pdfGenerator;
    private final MinioStorageService storageService;
    private final InvoiceEventPublisher eventPublisher;

    @Value("${aurapay.presigned-url.secret-key:aura-presigned-url-secret-key-2026}")
    private String presignedUrlSecretKey;

    @Value("${aurapay.presigned-url.expiration-minutes:15}")
    private int presignedUrlExpirationMinutes;

    @Value("${aurapay.presigned-url.base-url:http://localhost:8088}")
    private String baseUrl;

    @Transactional
    public Invoice processPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Processing PaymentSucceededEvent paymentIntentId={}, merchantId={}",
                event.paymentIntentId(), event.merchantId());

        UUID merchantUuid = UUID.fromString(event.merchantId());
        UUID paymentIntentUuid = UUID.fromString(event.paymentIntentId());

        // Idempotency check
        Optional<Invoice> existing = invoiceRepository.findByPaymentIntentIdAndInvoiceType(
                paymentIntentUuid, InvoiceType.INVOICE);
        if (existing.isPresent()) {
            log.info("Invoice already exists for paymentIntentId={}", event.paymentIntentId());
            return existing.get();
        }

        int currentYear = Year.now().getValue();
        long seq = invoiceRepository.countByMerchantIdAndInvoiceTypeAndYear(merchantUuid, InvoiceType.INVOICE, currentYear) + 1;
        String invoiceNumber = String.format("INV-%d-%06d", currentYear, seq);
        String objectKey = String.format("invoices/%s/%s.pdf", event.merchantId(), invoiceNumber);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .merchantId(merchantUuid)
                .paymentIntentId(paymentIntentUuid)
                .invoiceType(InvoiceType.INVOICE)
                .amountCents(event.amountCents())
                .currency(event.currency() != null ? event.currency() : "EUR")
                .pdfObjectKey(objectKey)
                .status(InvoiceStatus.GENERATED)
                .isTest(event.isTest())
                .build();

        try {
            // Generate PDF
            byte[] pdfBytes = pdfGenerator.generatePdf(invoice);

            // Upload PDF to MinIO
            storageService.uploadPdf(objectKey, pdfBytes);

            // Save Invoice metadata
            invoice = invoiceRepository.save(invoice);

            // Publish InvoiceGeneratedEvent
            InvoiceGeneratedEvent generatedEvent = new InvoiceGeneratedEvent(
                    "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                    "invoice.generated",
                    Instant.now(),
                    invoice.getId().toString(),
                    invoice.getInvoiceNumber(),
                    invoice.getMerchantId().toString(),
                    invoice.getPaymentIntentId().toString(),
                    invoice.getAmountCents(),
                    objectKey,
                    invoice.getIsTest()
            );
            eventPublisher.publishInvoiceGenerated(generatedEvent);

            log.info("Invoice created successfully ID={}, number={}", invoice.getId(), invoiceNumber);
            return invoice;
        } catch (Exception e) {
            log.error("Failed to generate invoice for paymentIntentId={}: {}", event.paymentIntentId(), e.getMessage(), e);
            invoice.setStatus(InvoiceStatus.FAILED);
            invoiceRepository.save(invoice);

            InvoiceGenerationFailedEvent failedEvent = new InvoiceGenerationFailedEvent(
                    "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                    "invoice.generation_failed",
                    Instant.now(),
                    event.merchantId(),
                    event.paymentIntentId(),
                    e.getMessage(),
                    invoice.getIsTest()
            );
            eventPublisher.publishInvoiceGenerationFailed(failedEvent);
            throw new RuntimeException("Invoice generation failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Invoice processRefundSucceeded(RefundSucceededEvent event) {
        log.info("Processing RefundSucceededEvent refundId={}, merchantId={}",
                event.refundId(), event.merchantId());

        UUID merchantUuid = UUID.fromString(event.merchantId());
        UUID refundUuid = UUID.fromString(event.refundId());
        UUID paymentIntentUuid = event.paymentIntentId() != null ? UUID.fromString(event.paymentIntentId()) : null;

        // Idempotency check
        Optional<Invoice> existing = invoiceRepository.findByRefundIdAndInvoiceType(
                refundUuid, InvoiceType.CREDIT_NOTE);
        if (existing.isPresent()) {
            log.info("Credit note already exists for refundId={}", event.refundId());
            return existing.get();
        }

        int currentYear = Year.now().getValue();
        long seq = invoiceRepository.countByMerchantIdAndInvoiceTypeAndYear(merchantUuid, InvoiceType.CREDIT_NOTE, currentYear) + 1;
        String invoiceNumber = String.format("CN-%d-%06d", currentYear, seq);
        String objectKey = String.format("credit-notes/%s/%s.pdf", event.merchantId(), invoiceNumber);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .merchantId(merchantUuid)
                .paymentIntentId(paymentIntentUuid)
                .refundId(refundUuid)
                .invoiceType(InvoiceType.CREDIT_NOTE)
                .amountCents(event.amountCents())
                .currency("EUR")
                .pdfObjectKey(objectKey)
                .status(InvoiceStatus.GENERATED)
                .isTest(event.isTest())
                .build();

        try {
            // Generate PDF
            byte[] pdfBytes = pdfGenerator.generatePdf(invoice);

            // Upload PDF to MinIO
            storageService.uploadPdf(objectKey, pdfBytes);

            // Save metadata
            invoice = invoiceRepository.save(invoice);

            // Publish InvoiceGeneratedEvent
            InvoiceGeneratedEvent generatedEvent = new InvoiceGeneratedEvent(
                    "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                    "invoice.generated",
                    Instant.now(),
                    invoice.getId().toString(),
                    invoice.getInvoiceNumber(),
                    invoice.getMerchantId().toString(),
                    invoice.getPaymentIntentId() != null ? invoice.getPaymentIntentId().toString() : null,
                    invoice.getAmountCents(),
                    objectKey,
                    invoice.getIsTest()
            );
            eventPublisher.publishInvoiceGenerated(generatedEvent);

            log.info("Credit note created successfully ID={}, number={}", invoice.getId(), invoiceNumber);
            return invoice;
        } catch (Exception e) {
            log.error("Failed to generate credit note for refundId={}: {}", event.refundId(), e.getMessage(), e);
            invoice.setStatus(InvoiceStatus.FAILED);
            invoiceRepository.save(invoice);

            InvoiceGenerationFailedEvent failedEvent = new InvoiceGenerationFailedEvent(
                    "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                    "invoice.generation_failed",
                    Instant.now(),
                    event.merchantId(),
                    event.paymentIntentId(),
                    e.getMessage(),
                    invoice.getIsTest()
            );
            eventPublisher.publishInvoiceGenerationFailed(failedEvent);
            throw new RuntimeException("Credit note generation failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoicesByMerchant(UUID merchantId, Pageable pageable) {
        return invoiceRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                .map(InvoiceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public InvoiceDownloadUrlResponse generatePresignedDownloadUrl(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.FAILED) {
            throw new BusinessException(AuraErrorCode.DOMAIN_RULE_VIOLATION, "Cannot download failed invoice PDF");
        }

        long expiresEpochSecond = Instant.now().plus(presignedUrlExpirationMinutes, ChronoUnit.MINUTES).getEpochSecond();
        String payload = invoiceId + ":" + expiresEpochSecond;
        String signature = HmacUtils.calculateHmacSha256(payload, presignedUrlSecretKey);

        String downloadUrl = String.format("%s/v1/invoices/%s/download?expires=%d&signature=%s",
                baseUrl, invoiceId, expiresEpochSecond, signature);

        return new InvoiceDownloadUrlResponse(invoiceId, downloadUrl, Instant.ofEpochSecond(expiresEpochSecond));
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdfWithSignature(UUID invoiceId, long expires, String signature) {
        long currentEpochSecond = Instant.now().getEpochSecond();
        if (currentEpochSecond > expires) {
            throw new BusinessException(AuraErrorCode.UNAUTHORIZED, "Presigned download URL has expired");
        }

        String payload = invoiceId + ":" + expires;
        boolean isValid = HmacUtils.verifyHmacSha256(payload, signature, presignedUrlSecretKey);
        if (!isValid) {
            throw new BusinessException(AuraErrorCode.UNAUTHORIZED, "Invalid signature for presigned download URL");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        return storageService.downloadPdf(invoice.getPdfObjectKey());
    }
}
