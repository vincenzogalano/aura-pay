package com.aurapay.core.exception;

/**
 * Standardized error codes across AuraPay microservices.
 */
public enum AuraErrorCode {
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
    UNAUTHORIZED("UNAUTHORIZED"),
    INVALID_API_KEY("INVALID_API_KEY"),
    DOMAIN_RULE_VIOLATION("DOMAIN_RULE_VIOLATION"),
    IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT"),
    VALIDATION_FAILED("VALIDATION_FAILED"),
    BANK_UNAVAILABLE("BANK_UNAVAILABLE"),
    VAULT_SERVICE_UNAVAILABLE("VAULT_SERVICE_UNAVAILABLE"),
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS"),
    EXPIRED_CARD("EXPIRED_CARD"),
    SUSPECTED_FRAUD("SUSPECTED_FRAUD"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    BUSINESS_ERROR("BUSINESS_ERROR");

    private final String code;

    AuraErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
