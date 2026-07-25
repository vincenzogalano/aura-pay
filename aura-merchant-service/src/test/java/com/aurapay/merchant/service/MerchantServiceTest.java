package com.aurapay.merchant.service;

import com.aurapay.core.exception.DomainRuleViolationException;
import com.aurapay.core.exception.ResourceNotFoundException;
import com.aurapay.merchant.domain.ApiKey;
import com.aurapay.merchant.domain.Merchant;
import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import com.aurapay.merchant.domain.enums.MerchantStatus;
import com.aurapay.merchant.dto.request.RegisterMerchantRequest;
import com.aurapay.merchant.dto.request.VerificationRequest;
import com.aurapay.merchant.dto.response.ApiKeyResponse;
import com.aurapay.merchant.dto.response.RegisterMerchantResponse;
import com.aurapay.merchant.dto.response.VerificationStatusResponse;
import com.aurapay.merchant.publisher.MerchantEventPublisher;
import com.aurapay.merchant.repository.ApiKeyRepository;
import com.aurapay.merchant.repository.MerchantRepository;
import com.aurapay.merchant.repository.MerchantWebhookConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private MerchantWebhookConfigRepository webhookConfigRepository;

    @Mock
    private VerificationService verificationService;

    @Mock
    private MerchantEventPublisher eventPublisher;

    @InjectMocks
    private MerchantService merchantService;

    private UUID merchantId;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        merchant = Merchant.builder()
                .id(merchantId)
                .businessName("Acme Corp")
                .vatNumber("12345678901")
                .email("info@acme.com")
                .status(MerchantStatus.PENDING_VERIFICATION)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Registrazione merchant con successo genera chiavi TEST ed invia evento Kafka")
    void registerMerchant_success() {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Acme Corp",
                "12345678901",
                "info@acme.com"
        );

        given(merchantRepository.existsByVatNumber(request.vatNumber())).willReturn(false);
        given(merchantRepository.existsByEmail(request.email())).willReturn(false);
        given(merchantRepository.save(any(Merchant.class))).willAnswer(invocation -> invocation.getArgument(0));

        RegisterMerchantResponse response = merchantService.registerMerchant(request);

        assertThat(response).isNotNull();
        assertThat(response.merchant().businessName()).isEqualTo("Acme Corp");
        assertThat(response.merchant().status()).isEqualTo(MerchantStatus.PENDING_VERIFICATION);
        assertThat(response.testApiKeys()).hasSize(2);
        assertThat(response.testApiKeys().get(0).keyPrefix()).startsWith("pk_test_");
        assertThat(response.testApiKeys().get(1).keyPrefix()).startsWith("sk_test_");

        verify(eventPublisher).publishMerchantCreated(any(), eq("Acme Corp"), eq("12345678901"), eq("info@acme.com"));
        verify(apiKeyRepository, times(2)).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("Registrazione fallisce con eccezione se la P.IVA è già registrata")
    void registerMerchant_duplicateVat_throwsException() {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Acme Corp",
                "12345678901",
                "info@acme.com"
        );

        given(merchantRepository.existsByVatNumber(request.vatNumber())).willReturn(true);

        assertThatThrownBy(() -> merchantService.registerMerchant(request))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Verifica KYB approvata sblocca stato VERIFIED e genera chiavi LIVE")
    void requestVerification_approved_unlocksVerifiedAndLiveKeys() {
        VerificationRequest req = new VerificationRequest(
                "REG123",
                "Via Roma 1",
                "Mario Rossi"
        );

        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(verificationService.evaluateVerification(eq("12345678901"), eq("info@acme.com"), any()))
                .willReturn(new VerificationService.VerificationResult(true, "KYB approved"));

        VerificationStatusResponse response = merchantService.requestVerification(merchantId, req);

        assertThat(response.status()).isEqualTo(MerchantStatus.VERIFIED);
        assertThat(response.liveApiKeys()).hasSize(2);
        assertThat(response.liveApiKeys().get(0).keyPrefix()).startsWith("pk_live_");

        verify(eventPublisher).publishMerchantVerified(eq(merchantId.toString()), eq("Acme Corp"), eq("12345678901"));
    }

    @Test
    @DisplayName("Verifica KYB respinta imposta stato VERIFICATION_REJECTED ed invia evento di rifiuto")
    void requestVerification_rejected_setsRejectedStatus() {
        VerificationRequest req = new VerificationRequest(
                "REG123",
                "Via Roma 1",
                "Mario Rossi"
        );

        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(verificationService.evaluateVerification(eq("12345678901"), eq("info@acme.com"), any()))
                .willReturn(new VerificationService.VerificationResult(false, "Generic email rejected"));

        VerificationStatusResponse response = merchantService.requestVerification(merchantId, req);

        assertThat(response.status()).isEqualTo(MerchantStatus.VERIFICATION_REJECTED);
        assertThat(response.liveApiKeys()).isEmpty();

        verify(eventPublisher).publishMerchantVerificationRejected(eq(merchantId.toString()), eq("Generic email rejected"));
    }

    @Test
    @DisplayName("Revoca API key ne imposta la data di revoca e pubblica l'evento")
    void revokeApiKey_success() {
        UUID keyId = UUID.randomUUID();
        ApiKey key = ApiKey.builder()
                .id(keyId)
                .merchantId(merchantId)
                .keyPrefix("pk_test_")
                .keyHash("hash")
                .environment(ApiKeyEnvironment.TEST)
                .build();

        given(apiKeyRepository.findById(keyId)).willReturn(Optional.of(key));
        given(apiKeyRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        ApiKeyResponse response = merchantService.revokeApiKey(merchantId, keyId);

        assertThat(response.active()).isFalse();
        assertThat(response.revokedAt()).isNotNull();

        verify(eventPublisher).publishApiKeyRevoked(eq(keyId.toString()), eq(merchantId.toString()), eq("pk_test_"), eq(true));
    }
}
