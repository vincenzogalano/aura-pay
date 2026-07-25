package com.aurapay.banksimulator.util;

public final class LatencyUtils {

    private LatencyUtils() {

    }

    public static void simulateLatency(long latencyMs) {
        if (latencyMs <= 0) return;
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
