package com.aurapay.ledger.controller;

import com.aurapay.ledger.dto.response.LedgerEntryResponse;
import com.aurapay.ledger.dto.response.MerchantBalanceResponse;
import com.aurapay.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ledger")
@RequiredArgsConstructor
@Slf4j
public class LedgerController {

    private final LedgerService ledgerService;
    @GetMapping("/accounts/{merchantId}/balance")
    public ResponseEntity<MerchantBalanceResponse> getBalance(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "false") boolean isTest
    ) {
        log.info("REST request to fetch balance for merchantId: {} (isTest: {})", merchantId, isTest);
        MerchantBalanceResponse response = ledgerService.getMerchantBalance(merchantId, isTest);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/entries/{merchantId}")
    public ResponseEntity<Page<LedgerEntryResponse>> getEntries(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "false") boolean isTest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to fetch ledger entries for merchantId: {} (isTest: {}, page: {}, size: {})",
                merchantId, isTest, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerEntryResponse> entries = ledgerService.getMerchantEntries(merchantId, isTest, pageable);
        return ResponseEntity.ok(entries);
    }
}
