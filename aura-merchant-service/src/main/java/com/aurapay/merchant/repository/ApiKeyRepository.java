package com.aurapay.merchant.repository;

import com.aurapay.merchant.domain.ApiKey;
import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByMerchantId(UUID merchantId);
    List<ApiKey> findByMerchantIdAndEnvironment(UUID merchantId, ApiKeyEnvironment environment);
}
