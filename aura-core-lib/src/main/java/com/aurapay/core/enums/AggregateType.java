package com.aurapay.core.enums;
public enum AggregateType {

    PAYMENT_INTENT("PaymentIntent"),
    MERCHANT("Merchant"),
    REFUND("Refund"),
    INVOICE("Invoice"),
    API_KEY("ApiKey");

    private final String value;

    AggregateType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AggregateType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AggregateType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
