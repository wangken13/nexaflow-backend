package com.vebcoding.trade.ai.api;

public record AiProviderStatus(String provider, String model, boolean configured, boolean fallbackEnabled) {
}
