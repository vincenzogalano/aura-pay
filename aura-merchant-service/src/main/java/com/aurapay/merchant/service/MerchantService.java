package com.aurapay.merchant.service;

import com.aurapay.core.exception.DomainRuleViolationException;
import com.aurapay.core.exception.ResourceNotFoundException;
import com.aurapay.core.security.ApiKeyGenerator;
import com.aurapay.merchant.domain.ApiKey;
import com.aurapay.merchant.domain.Merchant;
import com.aurapay.merchant.domain.MerchantWebhookConfig;
import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import com.aurapay.merchant.domain.enums.ApiKeyType;
import com.aurapay.merchant.domain.enums.MerchantStatus;
import com.aurapay.merchant.dto.request.*;
import com.aurapay.merchant.dto.response.*;
import com.aurapay.merchant.publisher.MerchantEventPublisher;
import com.aurapay.merchant.repository.ApiKeyRepository;
import com.aurapay.merchant.repository.MerchantRepository;
import com.aurapay.merchant.repository.MerchantWebhookConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final MerchantWebhookConfigRepository webhookConfigRepository;
    private final VerificationService verificationService;
    private final MerchantEventPublisher eventPublisher;

    @Transactional
    public RegisterMerchantResponse registerMerchant(RegisterMerchantRequest request) {
        if (merchantRepository.existsByVatNumber(request.getVatNumber())) {
            throw new DomainRuleViolationException("A merchant with VAT number '" + request.getVatNumber() + "' already exists");
        }
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new DomainRuleViolationException("A merchant with email '" + request.getEmail() + "' already exists");
        }

        Merchant merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .businessName(request.getBusinessName())
                .vatNumber(request.getVatNumber())
                .email(request.getEmail())
                .status(MerchantStatus.PENDING_VERIFICATION)
                .build();

        merchant = merchantRepository.save(merchant);
        log.info("Registered new merchant id={}, businessName={}", merchant.getId(), merchant.getBusinessName());

        // Generate immediate TEST API Key Pair (pk_test_..., sk_test_...)
        List<RawApiKeyDto> testKeys = generateAndSaveKeyPair(merchant.getId(), ApiKeyEnvironment.TEST);

        // Publish events
        eventPublisher.publishMerchantCreated(
                merchant.getId().toString(),
                merchant.getBusinessName(),
                merchant.getVatNumber(),
                merchant.getEmail()
        );

        return RegisterMerchantResponse.builder()
                .merchant(MerchantResponse.fromEntity(merchant))
                .testApiKeys(testKeys)
                .message("Merchant registered successfully. TEST API keys generated.")
                .build();
    }

    @Transactional
    public VerificationStatusResponse requestVerification(UUID merchantId, VerificationRequestDto request) {
        Merchant merchant = getMerchantEntity(merchantId);

        VerificationService.VerificationResult result = verificationService.evaluateVerification(
                merchant.getVatNumber(),
                merchant.getEmail(),
                request
        );

        if (result.approved()) {
            merchant.setStatus(MerchantStatus.VERIFIED);
            merchantRepository.save(merchant);
            log.info("Merchant id={} successfully VERIFIED via KYB check", merchantId);

            // Generate LIVE API keys
            List<RawApiKeyDto> liveKeys = generateAndSaveKeyPair(merchantId, ApiKeyEnvironment.LIVE);

            eventPublisher.publishMerchantVerified(
                    merchantId.toString(),
                    merchant.getBusinessName(),
                    merchant.getVatNumber()
            );

            return VerificationStatusResponse.builder()
                    .merchantId(merchantId)
                    .status(MerchantStatus.VERIFIED)
                    .details(result.reason())
                    .liveApiKeys(liveKeys)
                    .build();
        } else {
            merchant.setStatus(MerchantStatus.VERIFICATION_REJECTED);
            merchantRepository.save(merchant);
            log.warn("Merchant id={} KYB verification REJECTED: {}", merchantId, result.reason());

            eventPublisher.publishMerchantVerificationRejected(merchantId.toString(), result.reason());

            return VerificationStatusResponse.builder()
                    .merchantId(merchantId)
                    .status(MerchantStatus.VERIFICATION_REJECTED)
                    .details(result.reason())
                    .liveApiKeys(List.of())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(UUID merchantId) {
        return MerchantResponse.fromEntity(getMerchantEntity(merchantId));
    }

    @Transactional
    public MerchantResponse updateMerchant(UUID merchantId, UpdateMerchantRequest request) {
        Merchant merchant = getMerchantEntity(merchantId);
        merchant.setBusinessName(request.getBusinessName());
        merchant.setEmail(request.getEmail());
        merchant = merchantRepository.save(merchant);
        return MerchantResponse.fromEntity(merchant);
    }

    @Transactional(readOnly = true)
    public VerificationStatusResponse getVerificationStatus(UUID merchantId) {
        Merchant merchant = getMerchantEntity(merchantId);
        return VerificationStatusResponse.builder()
                .merchantId(merchantId)
                .status(merchant.getStatus())
                .details("Current merchant status is " + merchant.getStatus())
                .liveApiKeys(List.of())
                .build();
    }

    @Transactional
    public List<RawApiKeyDto> createApiKeyPair(UUID merchantId, CreateApiKeyRequest request) {
        Merchant merchant = getMerchantEntity(merchantId);

        if (request.getEnvironment() == ApiKeyEnvironment.LIVE && merchant.getStatus() != MerchantStatus.VERIFIED) {
            throw new DomainRuleViolationException("Cannot generate LIVE API keys for a merchant that is not VERIFIED (current status: " + merchant.getStatus() + ")");
        }

        return generateAndSaveKeyPair(merchantId, request.getEnvironment());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeys(UUID merchantId) {
        getMerchantEntity(merchantId); // check exists
        return apiKeyRepository.findByMerchantId(merchantId)
                .stream()
                .map(ApiKeyResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ApiKeyResponse revokeApiKey(UUID merchantId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey with id '" + keyId + "' was not found"));

        if (!apiKey.getMerchantId().equals(merchantId)) {
            throw new DomainRuleViolationException("ApiKey '" + keyId + "' does not belong to merchant '" + merchantId + "'");
        }

        if (apiKey.isRevoked()) {
            return ApiKeyResponse.fromEntity(apiKey);
        }

        apiKey.setRevokedAt(Instant.now());
        apiKey = apiKeyRepository.save(apiKey);
        log.info("Revoked ApiKey id={} for merchantId={}", keyId, merchantId);

        eventPublisher.publishApiKeyRevoked(
                keyId.toString(),
                merchantId.toString(),
                apiKey.getKeyPrefix(),
                apiKey.getEnvironment() == ApiKeyEnvironment.TEST
        );

        return ApiKeyResponse.fromEntity(apiKey);
    }

    @Transactional
    public WebhookConfigResponse configureWebhook(UUID merchantId, WebhookConfigRequest request) {
        getMerchantEntity(merchantId);

        MerchantWebhookConfig config = webhookConfigRepository.findByMerchantId(merchantId)
                .orElseGet(() -> MerchantWebhookConfig.builder()
                        .id(UUID.randomUUID())
                        .merchantId(merchantId)
                        .secretKey("whsec_" + UUID.randomUUID().toString().replace("-", ""))
                        .build());

        config.setTargetUrl(request.getTargetUrl());
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        } else {
            config.setEnabled(true);
        }

        config = webhookConfigRepository.save(config);
        log.info("Updated WebhookConfig for merchantId={}, targetUrl={}", merchantId, config.getTargetUrl());

        return WebhookConfigResponse.fromEntity(config);
    }

    @Transactional(readOnly = true)
    public WebhookConfigResponse getWebhookConfig(UUID merchantId) {
        getMerchantEntity(merchantId);
        MerchantWebhookConfig config = webhookConfigRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook configuration for merchant '" + merchantId + "' was not found"));
        return WebhookConfigResponse.fromEntity(config);
    }

    private Merchant getMerchantEntity(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant with id '" + merchantId + "' was not found"));
    }

    private List<RawApiKeyDto> generateAndSaveKeyPair(UUID merchantId, ApiKeyEnvironment environment) {
        String pkPrefix = environment == ApiKeyEnvironment.TEST ? ApiKeyGenerator.PK_TEST_PREFIX : ApiKeyGenerator.PK_LIVE_PREFIX;
        String skPrefix = environment == ApiKeyEnvironment.TEST ? ApiKeyGenerator.SK_TEST_PREFIX : ApiKeyGenerator.SK_LIVE_PREFIX;

        String rawPk = ApiKeyGenerator.generateApiKey(pkPrefix);
        String rawSk = ApiKeyGenerator.generateApiKey(skPrefix);

        String pkHash = ApiKeyGenerator.hashWithBcrypt(rawPk);
        String skHash = ApiKeyGenerator.hashWithBcrypt(rawSk);

        ApiKey pkEntity = ApiKey.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .keyPrefix(pkPrefix)
                .keyHash(pkHash)
                .environment(environment)
                .keyType(ApiKeyType.PUBLIC)
                .build();

        ApiKey skEntity = ApiKey.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .keyPrefix(skPrefix)
                .keyHash(skHash)
                .environment(environment)
                .keyType(ApiKeyType.SECRET)
                .build();

        apiKeyRepository.save(pkEntity);
        apiKeyRepository.save(skEntity);

        eventPublisher.publishApiKeyCreated(pkEntity.getId().toString(), merchantId.toString(), pkPrefix, environment.name(), environment == ApiKeyEnvironment.TEST);
        eventPublisher.publishApiKeyCreated(skEntity.getId().toString(), merchantId.toString(), skPrefix, environment.name(), environment == ApiKeyEnvironment.TEST);

        List<RawApiKeyDto> result = new ArrayList<>();
        result.add(RawApiKeyDto.builder()
                .id(pkEntity.getId())
                .environment(environment)
                .keyType(ApiKeyType.PUBLIC)
                .keyPrefix(pkPrefix)
                .rawKey(rawPk)
                .build());

        result.add(RawApiKeyDto.builder()
                .id(skEntity.getId())
                .environment(environment)
                .keyType(ApiKeyType.SECRET)
                .keyPrefix(skPrefix)
                .rawKey(rawSk)
                .build());

        return result;
    }
}
