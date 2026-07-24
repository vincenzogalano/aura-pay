package com.aurapay.webhook.repository;

import com.aurapay.webhook.domain.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {
    Optional<WebhookSubscription> findByMerchantId(UUID merchantId);
}
