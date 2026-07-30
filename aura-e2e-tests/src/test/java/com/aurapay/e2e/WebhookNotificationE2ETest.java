package com.aurapay.e2e;

import com.aurapay.core.security.HmacUtils;
import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
import com.aurapay.webhook.dto.request.WebhookSubscriptionRequest;
import com.aurapay.webhook.dto.response.WebhookSubscriptionResponse;
import com.aurapay.webhook.publisher.WebhookEventPublisher;
import com.aurapay.webhook.repository.WebhookDeliveryRepository;
import com.aurapay.webhook.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:webhook_e2edb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
class WebhookNotificationE2ETest {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @MockitoBean
    private WebhookEventPublisher webhookEventPublisher;

    @Test
    @DisplayName("E2E - Configurazione endpoint Webhook Merchant e registrazione secretKey")
    void e2e_webhookSubscription_createdSuccessfully() {
        UUID merchantId = UUID.randomUUID();

        WebhookSubscriptionRequest request = WebhookSubscriptionRequest.fromEventsString(
                merchantId,
                "https://merchant.example.com/webhooks",
                "whsec_testSecretKey12345",
                true,
                "payment.succeeded,refund.succeeded,merchant.verified"
        );

        WebhookSubscriptionResponse response = webhookService.createOrUpdateSubscription(request);

        assertThat(response).isNotNull();
        assertThat(response.merchantId()).isEqualTo(merchantId);
        assertThat(response.targetUrl()).isEqualTo("https://merchant.example.com/webhooks");
        assertThat(response.secretKey()).isEqualTo("whsec_testSecretKey12345");
        assertThat(response.enabled()).isTrue();
        assertThat(response.subscribedEvents()).contains("payment.succeeded");
    }

    @Test
    @DisplayName("E2E - Generazione e validazione firma HMAC-SHA256 per payload webhook")
    void e2e_webhookHmacSignature_matchesVerification() {
        String payload = "{\"eventId\":\"evt_123\",\"eventType\":\"payment.succeeded\",\"amountCents\":10000}";
        String secretKey = "whsec_superSecretKeyForHmacTesting";

        String signature = HmacUtils.calculateHmacSha256(payload, secretKey);

        assertThat(signature).isNotNull();
        assertThat(signature).hasSize(64); // Hex SHA-256 length

        boolean isValid = HmacUtils.verifyHmacSha256(payload, signature, secretKey);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("E2E - Tracciamento tentativo di consegna Webhook e ciclo di vita DeliveryStatus")
    void e2e_webhookDeliveryStatusTracking() {
        UUID merchantId = UUID.randomUUID();

        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .eventId("evt_wh_999")
                .eventType("payment.succeeded")
                .targetUrl("https://merchant.example.com/webhook")
                .payload("{\"status\":\"SUCCEEDED\"}")
                .status(DeliveryStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(5)
                .createdAt(Instant.now())
                .build();

        WebhookDelivery savedDelivery = webhookDeliveryRepository.save(delivery);

        assertThat(savedDelivery.getId()).isNotNull();
        assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(savedDelivery.getAttemptCount()).isEqualTo(0);

        // Update status to SUCCESS
        savedDelivery.setStatus(DeliveryStatus.SUCCESS);
        savedDelivery.setAttemptCount(1);
        savedDelivery.setDeliveredAt(Instant.now());
        savedDelivery.setHttpStatus(200);

        WebhookDelivery updatedDelivery = webhookDeliveryRepository.save(savedDelivery);
        assertThat(updatedDelivery.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(updatedDelivery.getHttpStatus()).isEqualTo(200);
    }
}
