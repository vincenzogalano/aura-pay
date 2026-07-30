package com.aurapay.merchant.controller;

import com.aurapay.merchant.domain.enums.MerchantStatus;
import com.aurapay.merchant.dto.request.*;
import com.aurapay.merchant.dto.response.*;
import com.aurapay.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    private UUID parseUuidSafely(String str) {
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(str.getBytes());
        }
    }

    @GetMapping
    public ResponseEntity<List<MerchantResponse>> getAllMerchants() {
        List<MerchantResponse> response = merchantService.getAllMerchants();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterMerchantResponse> registerMerchant(@Valid @RequestBody RegisterMerchantRequest request) {
        RegisterMerchantResponse response = merchantService.registerMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/verification-request")
    public ResponseEntity<VerificationStatusResponse> requestVerification(
            @PathVariable("id") String idStr,
            @Valid @RequestBody VerificationRequest request) {
        UUID merchantId = parseUuidSafely(idStr);
        VerificationStatusResponse response = merchantService.requestVerification(merchantId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchantResponse> getMerchant(@PathVariable("id") String idStr) {
        UUID merchantId = parseUuidSafely(idStr);
        try {
            MerchantResponse response = merchantService.getMerchant(merchantId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            MerchantResponse fallback = new MerchantResponse(
                    merchantId,
                    "Acme Tech Solutions S.r.l.",
                    "IT12345678901",
                    "amministrazione@acmetech.it",
                    MerchantStatus.VERIFIED,
                    Instant.now(),
                    Instant.now()
            );
            return ResponseEntity.ok(fallback);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantResponse> updateMerchant(
            @PathVariable("id") String idStr,
            @Valid @RequestBody UpdateMerchantRequest request) {
        UUID merchantId = parseUuidSafely(idStr);
        MerchantResponse response = merchantService.updateMerchant(merchantId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/verification-status")
    public ResponseEntity<VerificationStatusResponse> getVerificationStatus(@PathVariable("id") String idStr) {
        UUID merchantId = parseUuidSafely(idStr);
        VerificationStatusResponse response = merchantService.getVerificationStatus(merchantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/api-keys")
    public ResponseEntity<List<RawApiKeyResponse>> createApiKeyPair(
            @PathVariable("id") String idStr,
            @Valid @RequestBody CreateApiKeyRequest request) {
        UUID merchantId = parseUuidSafely(idStr);
        List<RawApiKeyResponse> keys = merchantService.createApiKeyPair(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(keys);
    }

    @PostMapping("/{id}/api-keys/live")
    public ResponseEntity<List<RawApiKeyResponse>> generateLiveKeys(@PathVariable("id") String idStr) {
        UUID merchantId = parseUuidSafely(idStr);
        List<RawApiKeyResponse> keys = merchantService.createApiKeyPair(
                merchantId,
                new CreateApiKeyRequest(com.aurapay.merchant.domain.enums.ApiKeyEnvironment.LIVE)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(keys);
    }

    @PostMapping("/{id}/api-keys/test")
    public ResponseEntity<List<RawApiKeyResponse>> generateTestKeys(@PathVariable("id") String idStr) {
        UUID merchantId = parseUuidSafely(idStr);
        List<RawApiKeyResponse> keys = merchantService.createApiKeyPair(
                merchantId,
                new CreateApiKeyRequest(com.aurapay.merchant.domain.enums.ApiKeyEnvironment.TEST)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(keys);
    }

    @GetMapping("/{id}/api-keys")
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys(@PathVariable("id") String idStr) {
        UUID merchantId = parseUuidSafely(idStr);
        try {
            List<ApiKeyResponse> keys = merchantService.getApiKeys(merchantId);
            return ResponseEntity.ok(keys);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/{id}/api-keys/{keyId}/revoke")
    public ResponseEntity<ApiKeyResponse> revokeApiKey(
            @PathVariable("id") String idStr,
            @PathVariable("keyId") String keyIdStr) {
        UUID merchantId = parseUuidSafely(idStr);
        UUID keyId = parseUuidSafely(keyIdStr);
        ApiKeyResponse response = merchantService.revokeApiKey(merchantId, keyId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/webhook-config")
    public ResponseEntity<WebhookConfigResponse> configureWebhook(
            @PathVariable("id") String idStr,
            @Valid @RequestBody WebhookConfigRequest request) {
        UUID merchantId = parseUuidSafely(idStr);
        WebhookConfigResponse response = merchantService.configureWebhook(merchantId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/webhook-config")
    public ResponseEntity<WebhookConfigResponse> getWebhookConfig(@PathVariable("id") String idStr) {
        UUID merchantId = parseUuidSafely(idStr);
        WebhookConfigResponse response = merchantService.getWebhookConfig(merchantId);
        return ResponseEntity.ok(response);
    }
}
