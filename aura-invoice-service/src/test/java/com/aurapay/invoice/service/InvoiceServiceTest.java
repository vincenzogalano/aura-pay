package com.aurapay.invoice.service;

import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.security.HmacUtils;
import com.aurapay.invoice.domain.Invoice;
import com.aurapay.invoice.domain.enums.InvoiceStatus;
import com.aurapay.invoice.domain.enums.InvoiceType;
import com.aurapay.invoice.dto.response.InvoiceDownloadUrlResponse;
import com.aurapay.invoice.publisher.InvoiceEventPublisher;
import com.aurapay.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoicePdfGenerator pdfGenerator;

    @Mock
    private MinioStorageService storageService;

    @Mock
    private InvoiceEventPublisher eventPublisher;

    @InjectMocks
    private InvoiceService invoiceService;

    private final String secretKey = "test-presigned-secret";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invoiceService, "presignedUrlSecretKey", secretKey);
        ReflectionTestUtils.setField(invoiceService, "presignedUrlExpirationMinutes", 15);
        ReflectionTestUtils.setField(invoiceService, "baseUrl", "http://localhost:8088");
    }

    @Test
    @DisplayName("Dovrebbe elaborare PaymentSucceededEvent creando la fattura, caricando il PDF su MinIO e salvando i metadati")
    void testProcessPaymentSucceededSuccess() {
        UUID merchantId = UUID.randomUUID();
        UUID paymentIntentId = UUID.randomUUID();

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                "evt_123",
                "payment.succeeded",
                Instant.now(),
                paymentIntentId.toString(),
                merchantId.toString(),
                10000L,
                150L,
                "EUR",
                "1111",
                "AUTH_123456",
                "customer@test.com",
                "Acquisto Prodotto",
                true
        );

        given(invoiceRepository.findByPaymentIntentIdAndInvoiceType(paymentIntentId, InvoiceType.INVOICE))
                .willReturn(Optional.empty());
        given(invoiceRepository.countByMerchantIdAndInvoiceTypeAndYear(eq(merchantId), eq(InvoiceType.INVOICE), any(Integer.class)))
                .willReturn(0L);
        given(pdfGenerator.generatePdf(any(Invoice.class)))
                .willReturn("DUMMY_PDF_BYTES".getBytes());
        given(invoiceRepository.save(any(Invoice.class)))
                .willAnswer(invocation -> {
                    Invoice inv = invocation.getArgument(0);
                    inv.setId(UUID.randomUUID());
                    return inv;
                });

        Invoice created = invoiceService.processPaymentSucceeded(event);

        assertThat(created).isNotNull();
        assertThat(created.getInvoiceNumber()).contains("INV-");
        assertThat(created.getStatus()).isEqualTo(InvoiceStatus.GENERATED);
        assertThat(created.getIsTest()).isTrue();

        verify(storageService).uploadPdf(eq(created.getPdfObjectKey()), any(byte[].class));
        verify(eventPublisher).publishInvoiceGenerated(any());
    }

    @Test
    @DisplayName("Dovrebbe essere idempotente e restituire la fattura gia' esistente per PaymentSucceededEvent")
    void testProcessPaymentSucceededIdempotent() {
        UUID merchantId = UUID.randomUUID();
        UUID paymentIntentId = UUID.randomUUID();

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                "evt_123",
                "payment.succeeded",
                Instant.now(),
                paymentIntentId.toString(),
                merchantId.toString(),
                10000L,
                150L,
                "EUR",
                "1111",
                "AUTH_123456",
                "customer@test.com",
                "Acquisto Prodotto",
                true
        );

        Invoice existing = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-2026-000001")
                .merchantId(merchantId)
                .paymentIntentId(paymentIntentId)
                .invoiceType(InvoiceType.INVOICE)
                .amountCents(10000L)
                .status(InvoiceStatus.GENERATED)
                .isTest(true)
                .build();

        given(invoiceRepository.findByPaymentIntentIdAndInvoiceType(paymentIntentId, InvoiceType.INVOICE))
                .willReturn(Optional.of(existing));

        Invoice result = invoiceService.processPaymentSucceeded(event);

        assertThat(result).isEqualTo(existing);
        verify(storageService, never()).uploadPdf(any(), any());
        verify(eventPublisher, never()).publishInvoiceGenerated(any());
    }

    @Test
    @DisplayName("Dovrebbe generare una presigned URL valida con scadenza a 15 minuti")
    void testGeneratePresignedDownloadUrl() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .invoiceNumber("INV-2026-000001")
                .status(InvoiceStatus.GENERATED)
                .build();

        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));

        InvoiceDownloadUrlResponse response = invoiceService.generatePresignedDownloadUrl(invoiceId);

        assertThat(response).isNotNull();
        assertThat(response.invoiceId()).isEqualTo(invoiceId);
        assertThat(response.downloadUrl()).contains("http://localhost:8088/v1/invoices/" + invoiceId + "/download?expires=");
        assertThat(response.downloadUrl()).contains("&signature=");
        assertThat(response.expiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Dovrebbe scaricare con successo il PDF per una firma presigned valida")
    void testDownloadPdfWithSignatureValid() {
        UUID invoiceId = UUID.randomUUID();
        long expires = Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond();
        String payload = invoiceId + ":" + expires;
        String validSignature = HmacUtils.calculateHmacSha256(payload, secretKey);

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .invoiceNumber("INV-2026-000001")
                .pdfObjectKey("invoices/mch/INV-2026-000001.pdf")
                .status(InvoiceStatus.GENERATED)
                .build();

        byte[] expectedPdf = "DUMMY_PDF_CONTENT".getBytes();

        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));
        given(storageService.downloadPdf("invoices/mch/INV-2026-000001.pdf")).willReturn(expectedPdf);

        byte[] downloaded = invoiceService.downloadPdfWithSignature(invoiceId, expires, validSignature);

        assertThat(downloaded).isEqualTo(expectedPdf);
    }

    @Test
    @DisplayName("Dovrebbe sollevare BusinessException se il presigned URL e' scaduto")
    void testDownloadPdfWithSignatureExpired() {
        UUID invoiceId = UUID.randomUUID();
        long expiredTimestamp = Instant.now().minus(5, ChronoUnit.MINUTES).getEpochSecond();
        String signature = "invalid_sig";

        assertThatThrownBy(() -> invoiceService.downloadPdfWithSignature(invoiceId, expiredTimestamp, signature))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Dovrebbe sollevare BusinessException se la firma del presigned URL non e' valida")
    void testDownloadPdfWithSignatureInvalid() {
        UUID invoiceId = UUID.randomUUID();
        long expires = Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond();
        String invalidSignature = "invalid_hmac_signature";

        assertThatThrownBy(() -> invoiceService.downloadPdfWithSignature(invoiceId, expires, invalidSignature))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid signature");
    }
}
