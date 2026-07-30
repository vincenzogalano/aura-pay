package com.aurapay.e2e;

import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.events.RefundSucceededEvent;
import com.aurapay.invoice.domain.Invoice;
import com.aurapay.invoice.domain.enums.InvoiceStatus;
import com.aurapay.invoice.domain.enums.InvoiceType;
import com.aurapay.invoice.dto.response.InvoiceDownloadUrlResponse;
import com.aurapay.invoice.publisher.InvoiceEventPublisher;
import com.aurapay.invoice.service.InvoicePdfGenerator;
import com.aurapay.invoice.service.InvoiceService;
import com.aurapay.invoice.service.MinioStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:invoice_e2edb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false"
})
@Transactional
class InvoiceGenerationAndMinIOE2ETest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoicePdfGenerator invoicePdfGenerator;

    @MockitoBean
    private MinioStorageService minioStorageService;

    @MockitoBean
    private InvoiceEventPublisher invoiceEventPublisher;

    @Test
    @DisplayName("E2E - Generazione automatica PDF Fattura INV-YYYY-XXXXXX con caricamento su MinIO")
    void e2e_invoiceGeneration_paymentSucceeded() {
        UUID merchantId = UUID.randomUUID();
        UUID paymentIntentId = UUID.randomUUID();

        PaymentSucceededEvent payEvent = new PaymentSucceededEvent(
                "evt_inv_01",
                "payment.succeeded",
                Instant.now(),
                paymentIntentId.toString(),
                merchantId.toString(),
                15000L,
                300L,
                "EUR",
                "1111",
                "AUTH_INV_01",
                true
        );

        Invoice invoice = invoiceService.processPaymentSucceeded(payEvent);

        assertThat(invoice).isNotNull();
        assertThat(invoice.getInvoiceNumber()).startsWith("INV-");
        assertThat(invoice.getInvoiceType()).isEqualTo(InvoiceType.INVOICE);
        assertThat(invoice.getAmountCents()).isEqualTo(15000L);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.GENERATED);
        assertThat(invoice.getIsTest()).isTrue();

        verify(minioStorageService).uploadPdf(any(), any());
    }

    @Test
    @DisplayName("E2E - Generazione Nota di Credito CN-YYYY-XXXXXX per evento di rimborso")
    void e2e_creditNoteGeneration_refundSucceeded() {
        UUID merchantId = UUID.randomUUID();
        UUID paymentIntentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();

        RefundSucceededEvent refundEvent = new RefundSucceededEvent(
                "evt_cn_01",
                "refund.succeeded",
                Instant.now(),
                refundId.toString(),
                paymentIntentId.toString(),
                merchantId.toString(),
                5000L,
                "customer_return",
                true
        );

        Invoice creditNote = invoiceService.processRefundSucceeded(refundEvent);

        assertThat(creditNote).isNotNull();
        assertThat(creditNote.getInvoiceNumber()).startsWith("CN-");
        assertThat(creditNote.getInvoiceType()).isEqualTo(InvoiceType.CREDIT_NOTE);
        assertThat(creditNote.getAmountCents()).isEqualTo(5000L);
        assertThat(creditNote.getStatus()).isEqualTo(InvoiceStatus.GENERATED);

        verify(minioStorageService).uploadPdf(any(), any());
    }

    @Test
    @DisplayName("E2E - Generazione Presigned URL a tempo (15 min) per il download sicuro del PDF")
    void e2e_presignedUrlGeneration_containsHmacSignature() {
        UUID merchantId = UUID.randomUUID();
        UUID paymentIntentId = UUID.randomUUID();

        PaymentSucceededEvent payEvent = new PaymentSucceededEvent(
                "evt_inv_02",
                "payment.succeeded",
                Instant.now(),
                paymentIntentId.toString(),
                merchantId.toString(),
                8000L,
                200L,
                "EUR",
                "1111",
                "AUTH_INV_02",
                true
        );

        Invoice invoice = invoiceService.processPaymentSucceeded(payEvent);
        InvoiceDownloadUrlResponse downloadUrlResponse = invoiceService.generatePresignedDownloadUrl(invoice.getId());

        assertThat(downloadUrlResponse).isNotNull();
        assertThat(downloadUrlResponse.downloadUrl()).contains("/v1/invoices/");
        assertThat(downloadUrlResponse.downloadUrl()).contains("signature=");
        assertThat(downloadUrlResponse.downloadUrl()).contains("expires=");
        assertThat(downloadUrlResponse.expiresAt()).isAfter(Instant.now());
    }
}
