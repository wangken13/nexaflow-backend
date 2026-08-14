package com.vebcoding.trade.ai.service;

import com.vebcoding.trade.ai.api.AiProviderStatus;
import reactor.core.publisher.Flux;

public interface AiProviderStrategy {
    String generate(String prompt);

    default Flux<String> stream(String prompt) {
        return Flux.just(generate(prompt));
    }

    default AiProviderStatus status() {
        return new AiProviderStatus("Custom", "unknown", true, true);
    }
}
