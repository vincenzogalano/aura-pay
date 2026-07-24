package com.aurapay.merchant.controller;

import com.aurapay.merchant.dto.request.*;
import com.aurapay.merchant.dto.response.*;
import com.aurapay.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/register")
    public ResponseEntity<RegisterMerchantResponse> registerMerchant(@Valid @RequestBody RegisterMerchantRequest request) {
        RegisterMerchantResponse response = merchantService.registerMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/verification-request")
    public ResponseEntity<VerificationStatusResponse> requestVerification(
            @PathVariable("id") UUID merchantId,
            @Valid @RequestBody VerificationRequestDto request) {
        VerificationStatusResponse response = merchantService.requestVerification(merchantId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchantResponse> getMerchant(@PathVariable("id") UUID merchantId) {
        MerchantResponse response = merchantService.getMerchant(merchantId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantResponse> updateMerchant(
            @PathVariable("id") UUID merchantId,
            @Valid @RequestBody UpdateMerchantRequest request) {
        MerchantResponse response = merchantService.updateMerchant(merchantId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/verification-status")
    public ResponseEntity<VerificationStatusResponse> getVerificationStatus(@PathVariable("id") UUID merchantId) {
        VerificationStatusResponse response = merchantService.getVerificationStatus(merchantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/api-keys")
    public ResponseEntity<List<RawApiKeyDto>> createApiKeyPair(
            @PathVariable("id") UUID merchantId,
            @Valid @RequestBody CreateApiKeyRequest request) {
        List<RawApiKeyDto> keys = merchantService.createApiKeyPair(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(keys);
    }

    @GetMapping("/{id}/api-keys")
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys(@PathVariable("id") UUID merchantId) {
        List<ApiKeyResponse> keys = merchantService.getApiKeys(merchantId);
        return ResponseEntity.ok(keys);
    }

    @PostMapping("/{id}/api-keys/{keyId}/revoke")
    public ResponseEntity<ApiKeyResponse> revokeApiKey(
            @PathVariable("id") UUID merchantId,
            @PathVariable("keyId") UUID keyId) {
        ApiKeyResponse response = merchantService.revokeApiKey(merchantId, keyId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/webhook-config")
    public ResponseEntity<WebhookConfigResponse> configureWebhook(
            @PathVariable("id") UUID merchantId,
            @Valid @RequestBody WebhookConfigRequest request) {
        WebhookConfigResponse response = merchantService.configureWebhook(merchantId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/webhook-config")
    public ResponseEntity<WebhookConfigResponse> getWebhookConfig(@PathVariable("id") UUID merchantId) {
        WebhookConfigResponse response = merchantService.getWebhookConfig(merchantId);
        return ResponseEntity.ok(response);
    }
}
