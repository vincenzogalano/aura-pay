package com.aurapay.webhook.controller;

import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.dto.request.ReplayRequest;
import com.aurapay.webhook.dto.request.WebhookSubscriptionRequest;
import com.aurapay.webhook.dto.response.WebhookDeliveryResponse;
import com.aurapay.webhook.dto.response.WebhookSubscriptionResponse;
import com.aurapay.webhook.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/subscriptions")
    public ResponseEntity<WebhookSubscriptionResponse> createSubscription(
            @Valid @RequestBody WebhookSubscriptionRequest request) {
        WebhookSubscriptionResponse response = webhookService.createOrUpdateSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/subscriptions/{merchantId}")
    public ResponseEntity<WebhookSubscriptionResponse> getSubscription(
            @PathVariable("merchantId") UUID merchantId) {
        WebhookSubscriptionResponse response = webhookService.getSubscription(merchantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/deliveries")
    public ResponseEntity<Page<WebhookDeliveryResponse>> getDeliveries(
            @RequestParam("merchantId") UUID merchantId,
            @RequestParam(value = "status", required = false) DeliveryStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Page<WebhookDeliveryResponse> deliveries = webhookService.getDeliveries(
                merchantId,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/deliveries/{id}")
    public ResponseEntity<WebhookDeliveryResponse> getDeliveryById(@PathVariable("id") UUID id) {
        WebhookDeliveryResponse response = webhookService.getDeliveryById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deliveries/{id}/replay")
    public ResponseEntity<WebhookDeliveryResponse> replayDelivery(@PathVariable("id") UUID id) {
        WebhookDeliveryResponse response = webhookService.replayDelivery(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/replay")
    public ResponseEntity<List<WebhookDeliveryResponse>> replayRange(@Valid @RequestBody ReplayRequest request) {
        List<WebhookDeliveryResponse> responses = webhookService.replayRange(request);
        return ResponseEntity.ok(responses);
    }
}
