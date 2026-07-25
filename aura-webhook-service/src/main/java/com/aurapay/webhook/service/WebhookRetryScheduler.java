package com.aurapay.webhook.service;

import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryScheduler {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcherService dispatcherService;

    @Scheduled(fixedDelayString = "${aurapay.webhook.retry-fixed-delay-ms:10000}")
    public void processPendingRetries() {
        Instant now = Instant.now();
        List<WebhookDelivery> pendingDeliveries = deliveryRepository.findByStatusAndNextRetryAtBefore(DeliveryStatus.FAILED, now, PageRequest.of(0, 50));
        if (!pendingDeliveries.isEmpty()) {
            log.info("WebhookRetryScheduler found {} pending retry deliveries to process", pendingDeliveries.size());
            for (WebhookDelivery delivery : pendingDeliveries) {
                dispatcherService.dispatchDelivery(delivery);
            }
        }
    }
}
