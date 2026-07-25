package com.aurapay.vault.validator;

public final class LuhnValidator {

    private LuhnValidator() {

    }

    public static boolean isValid(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }

        String sanitized = cardNumber.replaceAll("[\\s-]", "");

        if (!sanitized.matches("\\d{13,19}")) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;

        for (int i = sanitized.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(sanitized.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
}
