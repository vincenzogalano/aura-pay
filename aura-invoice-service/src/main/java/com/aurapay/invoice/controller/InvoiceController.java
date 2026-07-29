package com.aurapay.invoice.controller;

import com.aurapay.invoice.dto.response.InvoiceDownloadUrlResponse;
import com.aurapay.invoice.dto.response.InvoiceResponse;
import com.aurapay.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getInvoices(
            @RequestParam(value = "merchantId", required = false) String merchantId,
            @RequestParam(value = "isTest", required = false) Boolean isTest) {
        log.info("REST request to list invoices with isTest={}", isTest);
        List<InvoiceResponse> response = invoiceService.getAllInvoices(isTest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        log.info("REST request to fetch invoice metadata for id={}", id);
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<Page<InvoiceResponse>> getInvoicesByMerchant(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("REST request to list invoices for merchantId={}, page={}, size={}", merchantId, page, size);
        Page<InvoiceResponse> response = invoiceService.getInvoicesByMerchant(merchantId, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<InvoiceDownloadUrlResponse> generateDownloadUrl(@PathVariable UUID id) {
        log.info("REST request to generate presigned download URL for invoiceId={}", id);
        InvoiceDownloadUrlResponse response = invoiceService.generatePresignedDownloadUrl(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @RequestParam long expires,
            @RequestParam String signature) {
        log.info("REST request to download PDF file for invoiceId={} with signature", id);
        byte[] pdfBytes = invoiceService.downloadPdfWithSignature(id, expires, signature);

        InvoiceResponse metadata = invoiceService.getInvoiceById(id);
        String filename = metadata.invoiceNumber() != null ? metadata.invoiceNumber() + ".pdf" : "invoice.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }
}
