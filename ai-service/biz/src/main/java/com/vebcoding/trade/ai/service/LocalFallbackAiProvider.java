package com.vebcoding.trade.ai.service;

import org.springframework.stereotype.Component;

@Component
public class LocalFallbackAiProvider implements AiProviderStrategy {
    @Override
    public String generate(String prompt) {
        return "未配置模型 Key，已使用本地规则生成分析。";
    }
}
