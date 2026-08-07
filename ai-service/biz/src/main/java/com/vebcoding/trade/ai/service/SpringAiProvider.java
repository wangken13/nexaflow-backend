package com.vebcoding.trade.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SpringAiProvider implements AiProviderStrategy {
    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;
    private final LocalFallbackAiProvider fallbackAiProvider;

    public SpringAiProvider(ObjectProvider<ChatClient.Builder> chatClientBuilder,
                            LocalFallbackAiProvider fallbackAiProvider) {
        this.chatClientBuilder = chatClientBuilder;
        this.fallbackAiProvider = fallbackAiProvider;
    }

    @Override
    public String generate(String prompt) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            return fallbackAiProvider.generate(prompt);
        }
        try {
            return builder.build().prompt(prompt).call().content();
        } catch (Exception ex) {
            return "AI 璋冪敤澶辫触锛屽凡闄嶇骇涓烘湰鍦拌鍒欏垎鏋愶細" + ex.getMessage();
        }
    }
}