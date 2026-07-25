package com.aurapay.ledger.controller;

import com.aurapay.ledger.dto.response.LedgerEntryResponse;
import com.aurapay.ledger.dto.response.MerchantBalanceResponse;
import com.aurapay.ledger.service.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerController.class)
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerService ledgerService;

    @Test
    @DisplayName("Should return 200 OK with merchant available balance")
    void getBalance_ShouldReturnMerchantBalance() throws Exception {

        MerchantBalanceResponse response = new MerchantBalanceResponse(
                "mch_001",
                9700L,
                "EUR",
                true,
                Instant.now()
        );
        given(ledgerService.getMerchantBalance("mch_001", true)).willReturn(response);

        mockMvc.perform(get("/v1/ledger/accounts/mch_001/balance")
                        .param("isTest", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value("mch_001"))
                .andExpect(jsonPath("$.availableBalanceCents").value(9700))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.isTest").value(true));
    }

    @Test
    @DisplayName("Should return 200 OK with paginated merchant ledger entries")
    void getEntries_ShouldReturnPaginatedLedgerEntries() throws Exception {

        LedgerEntryResponse entry = new LedgerEntryResponse(
                "led_100",
                "mch_001",
                "PAYMENT",
                "CREDIT",
                "MERCHANT_AVAILABLE",
                "pi_100",
                9700L,
                "EUR",
                true,
                Instant.now()
        );
        PageImpl<LedgerEntryResponse> page = new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1);

        given(ledgerService.getMerchantEntries(eq("mch_001"), eq(true), any())).willReturn(page);

        mockMvc.perform(get("/v1/ledger/entries/mch_001")
                        .param("isTest", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entryId").value("led_100"))
                .andExpect(jsonPath("$.content[0].merchantId").value("mch_001"))
                .andExpect(jsonPath("$.content[0].transactionType").value("PAYMENT"))
                .andExpect(jsonPath("$.content[0].entryType").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].accountType").value("MERCHANT_AVAILABLE"))
                .andExpect(jsonPath("$.content[0].amountCents").value(9700));
    }
}
