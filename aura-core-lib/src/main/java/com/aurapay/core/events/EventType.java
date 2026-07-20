package com.aurapay.core.events;

/**
 * Enumeration of all domain event types and their corresponding Kafka topics.
 */
public enum EventType {

    MERCHANT_CREATED("aura.merchant.created.v1"),
    MERCHANT_VERIFIED("aura.merchant.verified.v1"),
    MERCHANT_VERIFICATION_REJECTED("aura.merchant.verification_rejected.v1"),

    API_KEY_CREATED("aura.apikey.created.v1"),
    API_KEY_REVOKED("aura.apikey.revoked.v1"),

    PAYMENT_INTENT_CREATED("aura.paymentintent.created.v1"),
    PAYMENT_PROCESSING("aura.payment.processing.v1"),
    PAYMENT_SUCCEEDED("aura.payment.succeeded.v1"),
    PAYMENT_FAILED("aura.payment.failed.v1"),

    REFUND_REQUESTED("aura.refund.requested.v1"),
    REFUND_SUCCEEDED("aura.refund.succeeded.v1"),
    REFUND_FAILED("aura.refund.failed.v1"),

    INVOICE_GENERATED("aura.invoice.generated.v1"),
    INVOICE_GENERATION_FAILED("aura.invoice.generation_failed.v1"),

    WEBHOOK_DELIVERY_SUCCEEDED("aura.webhook.delivery_succeeded.v1"),
    WEBHOOK_DELIVERY_DEAD_LETTERED("aura.webhook.delivery_dead_lettered.v1"),

    LEDGER_ENTRY_RECORDED("aura.ledger.entry_recorded.v1"),

    BANK_AUTHORIZATION_RESULT("aura.bank.authorization_result.v1");

    private final String topicName;

    EventType(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }

    public static EventType fromTopicName(String topicName) {
        if (topicName == null) {
            return null;
        }
        for (EventType type : values()) {
            if (type.topicName.equalsIgnoreCase(topicName)) {
                return type;
            }
        }
        return null;
    }
}
