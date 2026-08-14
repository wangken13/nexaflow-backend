package com.vebcoding.trade.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.ai.api.AiProviderStatus;
import com.vebcoding.trade.ai.service.LocalFallbackAiProvider;
import com.vebcoding.trade.ai.service.SpringAiProvider;
import org.junit.jupiter.api.Test;

class SpringAiProviderTest {
    @Test
    void fallsBackToLocalRulesWhenApiKeyIsMissing() {
        SpringAiProvider provider = new SpringAiProvider(
                new LocalFallbackAiProvider(), "", "https://api.deepseek.com", "deepseek-chat");

        assertThat(provider.generate("test prompt")).contains("本地规则");
        assertThat(provider.stream("test prompt").collectList().block()).containsExactly("未配置模型 Key，已使用本地规则生成分析。");
        assertThat(provider.status()).isEqualTo(new AiProviderStatus("DeepSeek", "deepseek-chat", false, true));
    }

    @Test
    void reportsConfiguredDeepSeekProviderWithoutCallingTheModel() {
        SpringAiProvider provider = new SpringAiProvider(
                new LocalFallbackAiProvider(), "test-api-key", "https://api.deepseek.com", "deepseek-chat");

        assertThat(provider.status()).isEqualTo(new AiProviderStatus("DeepSeek", "deepseek-chat", true, true));
    }
}
