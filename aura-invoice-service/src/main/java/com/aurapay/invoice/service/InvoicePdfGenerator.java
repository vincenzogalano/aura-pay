package com.aurapay.invoice.service;

import com.aurapay.invoice.domain.Invoice;
import com.aurapay.invoice.domain.enums.InvoiceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public byte[] generatePdf(Invoice invoice) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Header
                contentStream.beginText();
                contentStream.setFont(fontBold, 20);
                contentStream.setNonStrokingColor(new Color(26, 86, 219)); // Primary blue color
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("AuraPay — Payment Infrastructure");
                contentStream.endText();

                // Document Title
                contentStream.beginText();
                contentStream.setFont(fontBold, 16);
                contentStream.setNonStrokingColor(new Color(0, 0, 0));
                contentStream.newLineAtOffset(50, 715);
                String docTitle = invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE ? "NOTA DI CREDITO" : "FATTURA FISCALE";
                contentStream.showText(docTitle);
                contentStream.endText();

                // Metadata details
                int y = 675;
                drawLabelValue(contentStream, fontBold, fontRegular, "Numero Documento:", invoice.getInvoiceNumber(), 50, y);
                y -= 20;
                drawLabelValue(contentStream, fontBold, fontRegular, "Data Emissione:", DATE_FORMATTER.format(invoice.getCreatedAt()), 50, y);
                y -= 20;
                drawLabelValue(contentStream, fontBold, fontRegular, "ID Merchant:", invoice.getMerchantId().toString(), 50, y);
                y -= 20;

                if (invoice.getPaymentIntentId() != null) {
                    drawLabelValue(contentStream, fontBold, fontRegular, "Riferimento PaymentIntent:", invoice.getPaymentIntentId().toString(), 50, y);
                    y -= 20;
                }
                if (invoice.getRefundId() != null) {
                    drawLabelValue(contentStream, fontBold, fontRegular, "Riferimento Rimborso:", invoice.getRefundId().toString(), 50, y);
                    y -= 20;
                }

                // Divider line
                contentStream.setStrokingColor(new Color(200, 200, 200));
                contentStream.setLineWidth(1.0f);
                contentStream.moveTo(50, y);
                contentStream.lineTo(550, y);
                contentStream.stroke();
                y -= 30;

                // Table Header
                contentStream.beginText();
                contentStream.setFont(fontBold, 12);
                contentStream.setNonStrokingColor(new Color(0, 0, 0));
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("Descrizione");
                contentStream.newLineAtOffset(350, 0);
                contentStream.showText("Importo");
                contentStream.endText();

                y -= 15;
                contentStream.moveTo(50, y);
                contentStream.lineTo(550, y);
                contentStream.stroke();
                y -= 25;

                // Table Row
                contentStream.beginText();
                contentStream.setFont(fontRegular, 11);
                contentStream.newLineAtOffset(50, y);
                String description = invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE
                        ? "Storno / Rimborso Transazione"
                        : "Corrispettivo Transazione Pagamento";
                contentStream.showText(description);

                double amount = invoice.getAmountCents() / 100.00;
                String formattedAmount = String.format(Locale.US, "%.2f %s", amount, invoice.getCurrency());
                contentStream.newLineAtOffset(350, 0);
                contentStream.showText(formattedAmount);
                contentStream.endText();

                y -= 40;
                contentStream.moveTo(50, y);
                contentStream.lineTo(550, y);
                contentStream.stroke();
                y -= 30;

                // Total Summary
                contentStream.beginText();
                contentStream.setFont(fontBold, 14);
                contentStream.newLineAtOffset(300, y);
                contentStream.showText("TOTALE: " + formattedAmount);
                contentStream.endText();

                // Footer
                contentStream.beginText();
                contentStream.setFont(fontRegular, 9);
                contentStream.setNonStrokingColor(new Color(120, 120, 120));
                contentStream.newLineAtOffset(50, 40);
                contentStream.showText("AuraPay Ltd — Microservice Event-Driven Architecture Platform. All rights reserved.");
                contentStream.endText();

                // Watermark for Test mode
                if (Boolean.TRUE.equals(invoice.getIsTest())) {
                    contentStream.saveGraphicsState();
                    contentStream.beginText();
                    contentStream.setFont(fontBold, 38);
                    contentStream.setNonStrokingColor(new Color(220, 50, 50)); // Red color for TEST watermark
                    
                    // Rotate and position watermark across center of page
                    Matrix matrix = Matrix.getRotateInstance(Math.toRadians(35), 100, 320);
                    contentStream.setTextMatrix(matrix);
                    contentStream.showText("TEST - NON FISCALMENTE VALIDO");
                    contentStream.endText();
                    contentStream.restoreGraphicsState();
                }
            }

            document.save(baos);
            byte[] pdfBytes = baos.toByteArray();
            log.info("Successfully generated PDF for invoiceNumber={}, size={} bytes", invoice.getInvoiceNumber(), pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("Error generating PDF for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF invoice", e);
        }
    }

    private void drawLabelValue(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                                String label, String value, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(fontBold, 11);
        cs.setNonStrokingColor(new Color(0, 0, 0));
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.setFont(fontRegular, 11);
        cs.newLineAtOffset(180, 0);
        cs.showText(value != null ? value : "");
        cs.endText();
    }
}
