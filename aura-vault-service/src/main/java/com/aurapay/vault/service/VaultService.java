package com.aurapay.vault.service;

import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.exception.ResourceNotFoundException;
import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.security.ApiKeyGenerator;
import com.aurapay.core.security.CardMaskingUtils;
import com.aurapay.vault.client.HashiCorpVaultClient;
import com.aurapay.vault.dto.request.TokenizeRequest;
import com.aurapay.vault.dto.response.CardDetailsResponse;
import com.aurapay.vault.dto.response.TokenResponse;
import com.aurapay.vault.domain.CardToken;
import com.aurapay.vault.domain.enums.CardBrand;
import com.aurapay.vault.repository.CardTokenRepository;
import com.aurapay.vault.validator.LuhnValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class VaultService {

    private final CardTokenRepository cardTokenRepository;
    private final HashiCorpVaultClient vaultClient;
    private final int tokenTtlMinutes;

    public VaultService(
            CardTokenRepository cardTokenRepository,
            HashiCorpVaultClient vaultClient,
            @Value("${aurapay.vault.token-ttl-minutes:15}") int tokenTtlMinutes) {
        this.cardTokenRepository = cardTokenRepository;
        this.vaultClient = vaultClient;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    @Transactional
    public TokenResponse tokenize(TokenizeRequest request, String authHeader) {
        log.info("Tokenizing card for cardholder '{}'", request.cardholderName());

        String sanitizedPan = request.cardNumber().replaceAll("[\\s-]", "");
        if (!LuhnValidator.isValid(sanitizedPan)) {
            throw new BusinessException(AuraErrorCode.DOMAIN_RULE_VIOLATION, "Invalid card number (Luhn validation failed)");
        }

        validateExpiration(request.expirationMonth(), request.expirationYear());

        String apiKey = extractApiKey(authHeader);
        boolean isTest = apiKey.isEmpty() || ApiKeyGenerator.isTestKey(apiKey);

        CardBrand cardBrand = detectCardBrand(sanitizedPan);

        String maskedPan = CardMaskingUtils.maskPan(sanitizedPan);

        String encryptedPan = vaultClient.encrypt(sanitizedPan);
        String encryptedCvv = vaultClient.encrypt(request.cvv());

        String token = ApiKeyGenerator.generateApiKey("tok_");

        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenTtlMinutes, ChronoUnit.MINUTES);

        CardToken cardToken = CardToken.builder()
                .token(token)
                .encryptedPan(encryptedPan)
                .encryptedCvv(encryptedCvv)
                .cardholderName(request.cardholderName())
                .expirationMonth(request.expirationMonth())
                .expirationYear(request.expirationYear())
                .cardBrand(cardBrand)
                .maskedPan(maskedPan)
                .isTest(isTest)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        cardTokenRepository.save(cardToken);

        log.info("Card successfully tokenized. Token: {}, Brand: {}, ExpiresAt: {}", token, cardBrand, expiresAt);

        return new TokenResponse(
                token,
                maskedPan,
                cardBrand,
                request.cardholderName(),
                request.expirationMonth(),
                request.expirationYear(),
                now,
                expiresAt,
                !isTest
        );
    }

    @Transactional
    public CardDetailsResponse retrieve(String token, String authHeader) {
        log.info("Retrieving card details for token '{}'", token);

        CardToken cardToken = cardTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token", token));

        if (cardToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(AuraErrorCode.EXPIRED_CARD, "Token has expired");
        }

        if (cardToken.getUsedAt() != null) {
            throw new BusinessException(AuraErrorCode.DOMAIN_RULE_VIOLATION, "Token has already been used");
        }

        String apiKey = extractApiKey(authHeader);
        boolean keyIsTest = apiKey.isEmpty() || ApiKeyGenerator.isTestKey(apiKey);
        if (cardToken.isTest() != keyIsTest) {
            throw new BusinessException(AuraErrorCode.UNAUTHORIZED, "API Key environment does not match token environment");
        }

        String decryptedPan = vaultClient.decrypt(cardToken.getEncryptedPan());
        String decryptedCvv = vaultClient.decrypt(cardToken.getEncryptedCvv());

        cardToken.setUsedAt(Instant.now());
        cardTokenRepository.save(cardToken);

        log.info("Token '{}' successfully detokenized and marked as used", token);

        return new CardDetailsResponse(
                decryptedPan,
                cardToken.getCardholderName(),
                cardToken.getExpirationMonth(),
                cardToken.getExpirationYear(),
                decryptedCvv,
                cardToken.getMaskedPan(),
                cardToken.getCardBrand(),
                !cardToken.isTest()
        );
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void purgeExpiredTokens() {
        log.debug("Starting scheduled purge of expired card tokens...");
        cardTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private void validateExpiration(int month, int year) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        if (year < currentYear) {
            throw new BusinessException(AuraErrorCode.DOMAIN_RULE_VIOLATION, "Expiration year is in the past");
        } else if (year == currentYear && month < currentMonth) {
            throw new BusinessException(AuraErrorCode.DOMAIN_RULE_VIOLATION, "Expiration month is in the past");
        }
    }

    private CardBrand detectCardBrand(String pan) {
        if (pan.startsWith("4")) {
            return CardBrand.VISA;
        }
        if (pan.matches("^(5[1-5]|2[2-7])\\d*")) {
            return CardBrand.MASTERCARD;
        }
        if (pan.startsWith("34") || pan.startsWith("37")) {
            return CardBrand.AMEX;
        }
        if (pan.startsWith("6011") || pan.startsWith("65") || pan.matches("^622(12[6-9]|1[3-9][0-9]|[2-8][0-9][0-9]|9[0-1][0-9]|92[0-5]).*") || pan.matches("^64[4-9].*")) {
            return CardBrand.DISCOVER;
        }
        return CardBrand.UNKNOWN;
    }

    private String extractApiKey(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return "";
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader.trim();
    }
}
