package com.aurapay.webhook.repository;

import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Page<WebhookDelivery> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<WebhookDelivery> findByMerchantIdAndStatus(UUID merchantId, DeliveryStatus status, Pageable pageable);

    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(DeliveryStatus status, Instant now, Pageable pageable);

    @Query("SELECT d FROM WebhookDelivery d WHERE d.merchantId = :merchantId AND d.createdAt BETWEEN :startTime AND :endTime")
    List<WebhookDelivery> findForReplay(
            @Param("merchantId") UUID merchantId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
