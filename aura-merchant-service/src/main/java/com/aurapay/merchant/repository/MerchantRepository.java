package com.aurapay.merchant.repository;

import com.aurapay.merchant.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    boolean existsByVatNumber(String vatNumber);
    boolean existsByEmail(String email);
    Optional<Merchant> findByVatNumber(String vatNumber);
}
