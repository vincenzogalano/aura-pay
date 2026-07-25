package com.aurapay.e2e;

import com.aurapay.core.exception.DomainRuleViolationException;
import com.aurapay.merchant.domain.enums.MerchantStatus;
import com.aurapay.merchant.dto.request.RegisterMerchantRequest;
import com.aurapay.merchant.dto.request.VerificationRequest;
import com.aurapay.merchant.dto.response.ApiKeyResponse;
import com.aurapay.merchant.dto.response.RegisterMerchantResponse;
import com.aurapay.merchant.dto.response.VerificationStatusResponse;
import com.aurapay.merchant.publisher.MerchantEventPublisher;
import com.aurapay.merchant.service.MerchantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:merchant_e2edb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
class MerchantOnboardingE2ETest {

    @Autowired
    private MerchantService merchantService;

    @MockitoBean
    private MerchantEventPublisher merchantEventPublisher;

    @Test
    @DisplayName("E2E - Registrazione Merchant Sandbox genera chiavi TEST ed imposta stato PENDING_VERIFICATION")
    void e2e_merchantRegistration_sandboxKeysGenerated() {
        RegisterMerchantRequest registerReq = new RegisterMerchantRequest(
                "Apex Tech Solutions S.r.l.",
                "11223344556",
                "contact@apextech.io");

        RegisterMerchantResponse response = merchantService.registerMerchant(registerReq);

        assertThat(response).isNotNull();
        assertThat(response.merchant()).isNotNull();
        assertThat(response.merchant().businessName()).isEqualTo("Apex Tech Solutions S.r.l.");
        assertThat(response.merchant().status()).isEqualTo(MerchantStatus.PENDING_VERIFICATION);

        assertThat(response.testApiKeys()).hasSize(2);
        assertThat(response.testApiKeys())
                .anyMatch(k -> k.keyPrefix().startsWith("pk_test_"))
                .anyMatch(k -> k.keyPrefix().startsWith("sk_test_"));
    }

    @Test
    @DisplayName("E2E - Richiesta di Verifica KYB approvata sblocca stato VERIFIED e genera chiavi LIVE")
    void e2e_merchantVerification_approvedUnlocksLiveKeys() {
        RegisterMerchantRequest registerReq = new RegisterMerchantRequest(
                "Aura Commerce S.r.l.",
                "12345678903",
                "legal@auracommerce.com");

        RegisterMerchantResponse regResponse = merchantService.registerMerchant(registerReq);
        UUID merchantId = regResponse.merchant().id();

        VerificationRequest verificationReq = new VerificationRequest(
                "REA-543210",
                "Via Monte Napoleone 8, Milano",
                "Giuseppe Verdi");

        VerificationStatusResponse verificationResponse = merchantService.requestVerification(merchantId, verificationReq);

        assertThat(verificationResponse.status()).isEqualTo(MerchantStatus.VERIFIED);
        assertThat(verificationResponse.liveApiKeys()).hasSize(2);
        assertThat(verificationResponse.liveApiKeys())
                .anyMatch(k -> k.keyPrefix().startsWith("pk_live_"))
                .anyMatch(k -> k.keyPrefix().startsWith("sk_live_"));
    }

    @Test
    @DisplayName("E2E - Errore duplicazione P.IVA durante la registrazione merchant")
    void e2e_merchantRegistration_duplicateVat_throwsException() {
        RegisterMerchantRequest req1 = new RegisterMerchantRequest(
                "Store 1",
                "12345678901",
                "store1@test.com");
        merchantService.registerMerchant(req1);

        RegisterMerchantRequest req2 = new RegisterMerchantRequest(
                "Store 2",
                "12345678901",
                "store2@test.com");

        assertThatThrownBy(() -> merchantService.registerMerchant(req2))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("E2E - Revoca API Key disattiva la chiave e ne registra il timestamp")
    void e2e_apiKeyRevocation_success() {
        RegisterMerchantRequest registerReq = new RegisterMerchantRequest(
                "Revoke Test Ltd",
                "55443322110",
                "admin@revoketest.com");

        RegisterMerchantResponse regResponse = merchantService.registerMerchant(registerReq);
        UUID merchantId = regResponse.merchant().id();
        UUID keyId = regResponse.testApiKeys().get(0).id();

        ApiKeyResponse revokedResponse = merchantService.revokeApiKey(merchantId, keyId);

        assertThat(revokedResponse.active()).isFalse();
        assertThat(revokedResponse.revokedAt()).isNotNull();
    }
}
