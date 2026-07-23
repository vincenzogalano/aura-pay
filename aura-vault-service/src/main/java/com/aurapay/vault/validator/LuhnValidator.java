package com.aurapay.vault.validator;

public final class LuhnValidator {

    private LuhnValidator() {
        // Utility class
    }

    /**
     * Checks if a card number is valid according to the Luhn algorithm.
     *
     * @param cardNumber Card number to validate (can contain whitespace or dashes)
     * @return True if valid, false otherwise.
     */
    public static boolean isValid(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }

        // Remove spaces and dashes
        String sanitized = cardNumber.replaceAll("[\\s-]", "");

        // Must contain only digits and be between 13 and 19 characters
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
