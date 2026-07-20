package com.aurapay.core.security;

/**
 * Utility for PCI-compliant PAN (Primary Account Number) masking and extraction.
 */
public final class CardMaskingUtils {

    private CardMaskingUtils() {
        // Utility class
    }

    /**
     * Masks a PAN by keeping the BIN (first 6 digits) and last 4 digits visible, masking all middle digits with asterisks.
     * Example: "4532015899001111" -> "453201****1111"
     *
     * @param rawPan The unmasked card number string (with or without spaces/dashes).
     * @return Masked PAN string.
     */
    public static String maskPan(String rawPan) {
        if (rawPan == null || rawPan.isBlank()) {
            return "****";
        }
        String cleanPan = rawPan.replaceAll("\\s+|-", "");
        int length = cleanPan.length();

        if (length < 10) {
            if (length <= 4) {
                return "****" + cleanPan;
            }
            return cleanPan.substring(0, 2) + "*".repeat(length - 4) + cleanPan.substring(length - 2);
        }

        String firstSix = cleanPan.substring(0, 6);
        String lastFour = cleanPan.substring(length - 4);
        int middleCount = length - 10;

        return firstSix + "*".repeat(middleCount) + lastFour;
    }

    /**
     * Extracts the last 4 digits of a PAN.
     *
     * @param rawPan Unmasked or partially masked PAN string.
     * @return Last 4 digits.
     */
    public static String getLastFour(String rawPan) {
        if (rawPan == null || rawPan.isBlank()) {
            return "0000";
        }
        String cleanPan = rawPan.replaceAll("\\s+|-", "");
        if (cleanPan.length() <= 4) {
            return cleanPan;
        }
        return cleanPan.substring(cleanPan.length() - 4);
    }
}
