package com.aurapay.merchant.domain;

import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import com.aurapay.merchant.domain.enums.ApiKeyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private ApiKeyEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    private ApiKeyType keyType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
