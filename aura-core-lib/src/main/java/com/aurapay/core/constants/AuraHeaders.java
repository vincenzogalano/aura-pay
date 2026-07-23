package com.aurapay.core.constants;

/**
 * Standard HTTP Header constants used across AuraPay microservices.
 */
public final class AuraHeaders {

    public static final String CORRELATION_ID = "X-Correlation-ID";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String AUTHORIZATION = "Authorization";
    public static final String API_KEY = "X-Api-Key";

    private AuraHeaders() {
        // Utility class
    }
}
