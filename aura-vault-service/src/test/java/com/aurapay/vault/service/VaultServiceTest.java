package com.aurapay.vault.service;

import com.aurapay.core.exception.BusinessException;
import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.vault.client.HashiCorpVaultClient;
import com.aurapay.vault.dto.request.TokenizeRequest;
import com.aurapay.vault.dto.response.CardDetailsResponse;
import com.aurapay.vault.dto.response.TokenResponse;
import com.aurapay.vault.domain.CardToken;
import com.aurapay.vault.domain.enums.CardBrand;
import com.aurapay.vault.repository.CardTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vault Service Logic Tests")
class VaultServiceTest {

    @Mock
    private CardTokenRepository cardTokenRepository;

    @Mock
    private HashiCorpVaultClient vaultClient;

    private VaultService vaultService;

    @BeforeEach
    void setUp() {
        vaultService = new VaultService(cardTokenRepository, vaultClient, 15);
    }

    @Test
    @DisplayName("Should tokenize card successfully when details are valid")
    void testTokenizeHappyPath() {

        TokenizeRequest request = new TokenizeRequest(
                "4111 1111 1111 1111",
                "John Doe",
                12,
                2030,
                "123"
        );
        when(vaultClient.encrypt("4111111111111111")).thenReturn("vault:v1:encrypted_pan");
        when(vaultClient.encrypt("123")).thenReturn("vault:v1:encrypted_cvv");


        TokenResponse response = vaultService.tokenize(request, "Bearer pk_test_123");


        assertThat(response).isNotNull();
        assertThat(response.token()).startsWith("tok_");
        assertThat(response.maskedPan()).isEqualTo("411111******1111");
        assertThat(response.cardBrand()).isEqualTo(CardBrand.VISA);
        assertThat(response.livemode()).isFalse();

        ArgumentCaptor<CardToken> tokenCaptor = ArgumentCaptor.forClass(CardToken.class);
        verify(cardTokenRepository).save(tokenCaptor.capture());
        CardToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getEncryptedPan()).isEqualTo("vault:v1:encrypted_pan");
        assertThat(savedToken.getEncryptedCvv()).isEqualTo("vault:v1:encrypted_cvv");
        assertThat(savedToken.isTest()).isTrue();
    }

    @Test
    @DisplayName("Should throw BusinessException when card number fails Luhn check")
    void testTokenizeInvalidLuhn() {

        TokenizeRequest request = new TokenizeRequest(
                "4111 1111 1111 1112",
                "John Doe",
                12,
                2030,
                "123"
        );


        assertThatThrownBy(() -> vaultService.tokenize(request, "Bearer pk_test_123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Luhn validation failed");
    }

    @Test
    @DisplayName("Should retrieve card details successfully for valid unused token")
    void testRetrieveHappyPath() {

        String token = "tok_test_token";
        CardToken cardToken = CardToken.builder()
                .token(token)
                .encryptedPan("vault:v1:encrypted_pan")
                .encryptedCvv("vault:v1:encrypted_cvv")
                .cardholderName("John Doe")
                .expirationMonth(12)
                .expirationYear(2030)
                .cardBrand(CardBrand.VISA)
                .maskedPan("411111******1111")
                .isTest(true)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        when(cardTokenRepository.findByToken(token)).thenReturn(Optional.of(cardToken));
        when(vaultClient.decrypt("vault:v1:encrypted_pan")).thenReturn("4111111111111111");
        when(vaultClient.decrypt("vault:v1:encrypted_cvv")).thenReturn("123");


        CardDetailsResponse response = vaultService.retrieve(token, "Bearer sk_test_123");


        assertThat(response).isNotNull();
        assertThat(response.cardNumber()).isEqualTo("4111111111111111");
        assertThat(response.cvv()).isEqualTo("123");
        assertThat(response.livemode()).isFalse();

        assertThat(cardToken.getUsedAt()).isNotNull();
        verify(cardTokenRepository).save(cardToken);
    }

    @Test
    @DisplayName("Should throw BusinessException when token is expired")
    void testRetrieveExpiredToken() {

        String token = "tok_test_token";
        CardToken cardToken = CardToken.builder()
                .token(token)
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();

        when(cardTokenRepository.findByToken(token)).thenReturn(Optional.of(cardToken));


        assertThatThrownBy(() -> vaultService.retrieve(token, "Bearer sk_test_123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token has expired");
    }

    @Test
    @DisplayName("Should throw BusinessException when token has already been retrieved")
    void testRetrieveAlreadyUsedToken() {

        String token = "tok_test_token";
        CardToken cardToken = CardToken.builder()
                .token(token)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .usedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        when(cardTokenRepository.findByToken(token)).thenReturn(Optional.of(cardToken));


        assertThatThrownBy(() -> vaultService.retrieve(token, "Bearer sk_test_123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token has already been used");
    }

    @Test
    @DisplayName("Should throw Unauthorized when API key environment mismatches token environment")
    void testRetrieveEnvMismatch() {

        String token = "tok_test_token";
        CardToken cardToken = CardToken.builder()
                .token(token)
                .isTest(true)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        when(cardTokenRepository.findByToken(token)).thenReturn(Optional.of(cardToken));


        assertThatThrownBy(() -> vaultService.retrieve(token, "Bearer sk_live_123"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuraErrorCode.UNAUTHORIZED.getCode());
    }
}
