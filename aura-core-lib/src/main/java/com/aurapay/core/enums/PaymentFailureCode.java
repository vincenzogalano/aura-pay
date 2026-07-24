package com.aurapay.core.enums;

/**
 * Enumeration of payment failure codes used in PaymentFailedEvent and Outbox events.
 */
public enum PaymentFailureCode {

    BANK_DECLINED,
    PAYMENT_CANCELLED,
    INSUFFICIENT_FUNDS,
    EXPIRED_CARD,
    SUSPECTED_FRAUD,
    INVALID_TOKEN,
    SYSTEM_ERROR
}
