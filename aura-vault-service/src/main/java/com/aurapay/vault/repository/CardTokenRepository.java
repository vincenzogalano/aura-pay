package com.aurapay.vault.repository;

import com.aurapay.vault.domain.CardToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardTokenRepository extends JpaRepository<CardToken, UUID> {
    Optional<CardToken> findByToken(String token);
    void deleteByExpiresAtBefore(Instant now);
}
