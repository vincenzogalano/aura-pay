package com.aurapay.invoice.service;

import com.aurapay.invoice.domain.Invoice;
import com.aurapay.invoice.domain.enums.InvoiceStatus;
import com.aurapay.invoice.domain.enums.InvoiceType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePdfGeneratorTest {

    private InvoicePdfGenerator pdfGenerator;

    @BeforeEach
    void setUp() {
        pdfGenerator = new InvoicePdfGenerator();
    }

    @Test
    @DisplayName("Dovrebbe generare un file PDF non vuoto per una Fattura in ambiente Sandbox con Watermark TEST")
    void testGeneratePdfForTestInvoice() throws IOException {
        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-2026-000001")
                .merchantId(UUID.randomUUID())
                .paymentIntentId(UUID.randomUUID())
                .invoiceType(InvoiceType.INVOICE)
                .amountCents(12500L)
                .currency("EUR")
                .status(InvoiceStatus.GENERATED)
                .isTest(true)
                .createdAt(Instant.now())
                .build();

        byte[] pdfBytes = pdfGenerator.generatePdf(invoice);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);

        try (PDDocument document = Loader.loadPdf(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertThat(text).contains("INV-2026-000001");
            assertThat(text).contains("FATTURA FISCALE");
            assertThat(text).contains("125.00 EUR");
            assertThat(text).contains("TEST");
        }
    }

    @Test
    @DisplayName("Dovrebbe generare un PDF valido per una Nota di Credito senza watermark se isTest e' false")
    void testGeneratePdfForLiveCreditNote() throws IOException {
        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("CN-2026-000001")
                .merchantId(UUID.randomUUID())
                .refundId(UUID.randomUUID())
                .invoiceType(InvoiceType.CREDIT_NOTE)
                .amountCents(5000L)
                .currency("EUR")
                .status(InvoiceStatus.GENERATED)
                .isTest(false)
                .createdAt(Instant.now())
                .build();

        byte[] pdfBytes = pdfGenerator.generatePdf(invoice);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);

        try (PDDocument document = Loader.loadPdf(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertThat(text).contains("CN-2026-000001");
            assertThat(text).contains("NOTA DI CREDITO");
            assertThat(text).contains("50.00 EUR");
            assertThat(text).doesNotContain("NON FISCALMENTE VALIDO");
        }
    }

    private static class Loader {
        public static PDDocument loadPdf(byte[] pdfBytes) throws IOException {
            return org.apache.pdfbox.Loader.loadPDF(pdfBytes);
        }
    }
}
