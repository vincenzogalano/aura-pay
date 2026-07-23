package com.aurapay.vault.domain;

import com.aurapay.vault.domain.enums.CardBrand;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "encrypted_pan", nullable = false, length = 512)
    private String encryptedPan;

    @Column(name = "encrypted_cvv", nullable = false, length = 256)
    private String encryptedCvv;

    @Column(name = "cardholder_name", nullable = false)
    private String cardholderName;

    @Column(name = "expiration_month", nullable = false)
    private Integer expirationMonth;

    @Column(name = "expiration_year", nullable = false)
    private Integer expirationYear;

    @Column(name = "card_brand", nullable = false)
    @Enumerated(EnumType.STRING)
    private CardBrand cardBrand;

    @Column(name = "masked_pan", nullable = false)
    private String maskedPan;

    @Column(name = "is_test", nullable = false)
    private boolean isTest;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;
}
