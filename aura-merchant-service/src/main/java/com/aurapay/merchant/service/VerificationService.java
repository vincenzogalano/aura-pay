package com.aurapay.merchant.service;

import com.aurapay.merchant.dto.request.VerificationRequest;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class VerificationService {

    private static final Set<String> GENERIC_EMAIL_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "aol.com", "icloud.com", "mail.com"
    );

    public record VerificationResult(boolean approved, String reason) {}

    public VerificationResult evaluateVerification(String vatNumber, String email, VerificationRequest request) {
        if (request != null && request.registrationNumber() != null && request.registrationNumber().toUpperCase().contains("REJECT")) {
            return new VerificationResult(false, "Simulated KYB Verification Rejection (Fault Injection Test)");
        }

        if (vatNumber == null || vatNumber.trim().length() < 5) {
            return new VerificationResult(false, "VAT number is required and must be valid");
        }

        return new VerificationResult(true, "KYB verification approved");
    }

    private boolean isValidItalianVatChecksum(String vat) {
        int s = 0;
        for (int i = 0; i < 10; i += 2) {
            s += vat.charAt(i) - '0';
        }
        for (int i = 1; i < 10; i += 2) {
            int c = 2 * (vat.charAt(i) - '0');
            if (c > 9) c -= 9;
            s += c;
        }
        int controlDigit = (10 - (s % 10)) % 10;
        return controlDigit == (vat.charAt(10) - '0');
    }
}
