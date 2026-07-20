package com.aurapay.core.exception;

public class DomainRuleViolationException extends BusinessException {

    public DomainRuleViolationException(String message) {
        super("DOMAIN_RULE_VIOLATION", message);
    }
}
