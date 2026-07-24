package com.aurapay.webhook.service;

import com.aurapay.core.exception.ResourceNotFoundException;
import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.WebhookSubscription;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.dto.request.ReplayRequest;
import com.aurapay.webhook.dto.request.WebhookSubscriptionRequest;
import com.aurapay.webhook.dto.response.WebhookDeliveryResponse;
import com.aurapay.webhook.dto.response.WebhookSubscriptionResponse;
import com.aurapay.webhook.repository.WebhookDeliveryRepository;
import com.aurapay.webhook.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcherService dispatcherService;

    @Transactional
    public WebhookSubscriptionResponse createOrUpdateSubscription(WebhookSubscriptionRequest request) {
        WebhookSubscription sub = subscriptionRepository.findByMerchantId(request.getMerchantId())
                .orElseGet(() -> WebhookSubscription.builder()
                        .id(UUID.randomUUID())
                        .merchantId(request.getMerchantId())
                        .secretKey(request.getSecretKey() != null ? request.getSecretKey() : "whsec_" + UUID.randomUUID().toString().replace("-", ""))
                        .build());

        sub.setTargetUrl(request.getTargetUrl());
        if (request.getSecretKey() != null && !request.getSecretKey().isBlank()) {
            sub.setSecretKey(request.getSecretKey());
        }
        if (request.getEnabled() != null) {
            sub.setEnabled(request.getEnabled());
        } else {
            sub.setEnabled(true);
        }
        if (request.getSubscribedEvents() != null) {
            sub.setSubscribedEvents(request.getSubscribedEvents());
        }

        sub = subscriptionRepository.save(sub);
        log.info("Saved WebhookSubscription for merchantId={}, targetUrl={}", sub.getMerchantId(), sub.getTargetUrl());
        return WebhookSubscriptionResponse.fromEntity(sub);
    }

    @Transactional(readOnly = true)
    public WebhookSubscriptionResponse getSubscription(UUID merchantId) {
        WebhookSubscription sub = subscriptionRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookSubscription for merchant '" + merchantId + "' was not found"));
        return WebhookSubscriptionResponse.fromEntity(sub);
    }

    @Transactional(readOnly = true)
    public Page<WebhookDeliveryResponse> getDeliveries(UUID merchantId, DeliveryStatus statusFilter, Pageable pageable) {
        Page<WebhookDelivery> page;
        if (statusFilter != null) {
            page = deliveryRepository.findByMerchantIdAndStatus(merchantId, statusFilter, pageable);
        } else {
            page = deliveryRepository.findByMerchantId(merchantId, pageable);
        }
        return page.map(WebhookDeliveryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public WebhookDeliveryResponse getDeliveryById(UUID deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookDelivery with id '" + deliveryId + "' was not found"));
        return WebhookDeliveryResponse.fromEntity(delivery);
    }

    @Transactional
    public WebhookDeliveryResponse replayDelivery(UUID deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookDelivery with id '" + deliveryId + "' was not found"));

        log.info("Manual replay triggered for deliveryId={}", deliveryId);
        delivery.setAttemptCount(0);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setNextRetryAt(null);
        delivery = deliveryRepository.save(delivery);

        dispatcherService.dispatchDelivery(delivery);
        return WebhookDeliveryResponse.fromEntity(delivery);
    }

    @Transactional
    public List<WebhookDeliveryResponse> replayRange(ReplayRequest request) {
        List<WebhookDelivery> deliveries = deliveryRepository.findForReplay(
                request.getMerchantId(),
                request.getStartTime(),
                request.getEndTime()
        );

        log.info("Triggered range replay for merchantId={}, found {} deliveries", request.getMerchantId(), deliveries.size());
        for (WebhookDelivery delivery : deliveries) {
            delivery.setAttemptCount(0);
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextRetryAt(null);
            deliveryRepository.save(delivery);
            dispatcherService.dispatchDelivery(delivery);
        }

        return deliveries.stream().map(WebhookDeliveryResponse::fromEntity).toList();
    }
}
