package com.aurapay.core.events;
public enum BankResponseCode {
    APPROVED("00", "Approved"),
    INSUFFICIENT_FUNDS("51", "INSUFFICIENT_FUNDS"),
    EXPIRED_CARD("54", "EXPIRED_CARD"),
    SUSPECTED_FRAUD("59", "SUSPECTED_FRAUD"),
    SYSTEM_ERROR("96", "SYSTEM_ERROR");

    private final String code;
    private final String description;

    BankResponseCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
