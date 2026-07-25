package com.aurapay.core.security;
public final class CardMaskingUtils {

    private CardMaskingUtils() {
    }
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
