package com.aurapay.vault.controller;

import com.aurapay.core.constants.AuraHeaders;
import com.aurapay.vault.dto.request.RetrieveRequest;
import com.aurapay.vault.dto.request.TokenizeRequest;
import com.aurapay.vault.dto.response.CardDetailsResponse;
import com.aurapay.vault.dto.response.TokenResponse;
import com.aurapay.vault.service.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tokens")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @PostMapping
    public ResponseEntity<TokenResponse> tokenize(
            @RequestBody @Valid TokenizeRequest request,
            @RequestHeader(value = AuraHeaders.AUTHORIZATION, required = false) String authHeader) {
        TokenResponse response = vaultService.tokenize(request, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/retrieve")
    public ResponseEntity<CardDetailsResponse> retrieve(
            @RequestBody @Valid RetrieveRequest request,
            @RequestHeader(value = AuraHeaders.AUTHORIZATION, required = false) String authHeader) {
        CardDetailsResponse response = vaultService.retrieve(request.token(), authHeader);
        return ResponseEntity.ok(response);
    }
}
