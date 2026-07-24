package com.aurapay.invoice.controller;

import com.aurapay.invoice.domain.enums.InvoiceStatus;
import com.aurapay.invoice.domain.enums.InvoiceType;
import com.aurapay.invoice.dto.response.InvoiceDownloadUrlResponse;
import com.aurapay.invoice.dto.response.InvoiceResponse;
import com.aurapay.invoice.service.InvoiceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    @DisplayName("GET /v1/invoices/{id} - Dovrebbe restituire i metadati della fattura")
    void testGetInvoiceByIdSuccess() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID paymentIntentId = UUID.randomUUID();

        InvoiceResponse response = new InvoiceResponse(
                invoiceId,
                "INV-2026-000001",
                merchantId,
                paymentIntentId,
                null,
                InvoiceType.INVOICE,
                10000L,
                "EUR",
                "invoices/mch/INV-2026-000001.pdf",
                InvoiceStatus.GENERATED,
                true,
                Instant.now()
        );

        given(invoiceService.getInvoiceById(invoiceId)).willReturn(response);

        mockMvc.perform(get("/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-000001"))
                .andExpect(jsonPath("$.invoiceType").value("INVOICE"))
                .andExpect(jsonPath("$.amountCents").value(10000))
                .andExpect(jsonPath("$.isTest").value(true));
    }

    @Test
    @DisplayName("GET /v1/invoices/merchant/{merchantId} - Dovrebbe restituire l'elenco paginato delle fatture")
    void testGetInvoicesByMerchantSuccess() throws Exception {
        UUID merchantId = UUID.randomUUID();

        InvoiceResponse response = new InvoiceResponse(
                UUID.randomUUID(),
                "INV-2026-000001",
                merchantId,
                UUID.randomUUID(),
                null,
                InvoiceType.INVOICE,
                10000L,
                "EUR",
                "invoices/mch/INV-2026-000001.pdf",
                InvoiceStatus.GENERATED,
                true,
                Instant.now()
        );

        given(invoiceService.getInvoicesByMerchant(eq(merchantId), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/v1/invoices/merchant/{merchantId}", merchantId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-2026-000001"));
    }

    @Test
    @DisplayName("GET /v1/invoices/{id}/download-url - Dovrebbe restituire la risposta con presigned download URL")
    void testGenerateDownloadUrlSuccess() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        String downloadUrl = "http://localhost:8088/v1/invoices/" + invoiceId + "/download?expires=123456789&signature=abc";

        InvoiceDownloadUrlResponse response = new InvoiceDownloadUrlResponse(
                invoiceId,
                downloadUrl,
                Instant.now().plusSeconds(900)
        );

        given(invoiceService.generatePresignedDownloadUrl(invoiceId)).willReturn(response);

        mockMvc.perform(get("/v1/invoices/{id}/download-url", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()))
                .andExpect(jsonPath("$.downloadUrl").value(downloadUrl));
    }

    @Test
    @DisplayName("GET /v1/invoices/{id}/download - Dovrebbe restituire il file PDF in streaming per presigned URL valida")
    void testDownloadPdfSuccess() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        long expires = 123456789L;
        String signature = "valid_signature";
        byte[] pdfBytes = "%PDF-1.4 dummy pdf bytes".getBytes();

        InvoiceResponse response = new InvoiceResponse(
                invoiceId,
                "INV-2026-000001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                InvoiceType.INVOICE,
                10000L,
                "EUR",
                "invoices/mch/INV-2026-000001.pdf",
                InvoiceStatus.GENERATED,
                true,
                Instant.now()
        );

        given(invoiceService.getInvoiceById(invoiceId)).willReturn(response);
        given(invoiceService.downloadPdfWithSignature(invoiceId, expires, signature)).willReturn(pdfBytes);

        mockMvc.perform(get("/v1/invoices/{id}/download", invoiceId)
                        .param("expires", String.valueOf(expires))
                        .param("signature", signature))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"INV-2026-000001.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }
}
