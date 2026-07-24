package com.aurapay.merchant.repository;

import com.aurapay.merchant.domain.MerchantWebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantWebhookConfigRepository extends JpaRepository<MerchantWebhookConfig, UUID> {
    Optional<MerchantWebhookConfig> findByMerchantId(UUID merchantId);
}
