package com.vebcoding.trade.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Primary
public class SpringAiProvider implements AiProviderStrategy {
    private final LocalFallbackAiProvider fallbackAiProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${spring.ai.openai.api-key:}")
    private String configuredApiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    public SpringAiProvider(LocalFallbackAiProvider fallbackAiProvider, ObjectMapper objectMapper) {
        this.fallbackAiProvider = fallbackAiProvider;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String generate(String prompt) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            return fallbackAiProvider.generate(prompt);
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.3);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "AI 调用失败，已降级为本地规则分析：HTTP " + response.statusCode();
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() ? fallbackAiProvider.generate(prompt) : content.asText();
        } catch (HttpTimeoutException ex) {
            return "AI 模型响应超时，已降级为本地规则分析。";
        } catch (java.net.ConnectException ex) {
            return "无法连接 AI 模型服务，已降级为本地规则分析。";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "AI 分析任务被中断，已降级为本地规则分析。";
        } catch (Exception ex) {
            return "AI 模型响应格式异常，已降级为本地规则分析。";
        }
    }

    private String resolveApiKey() {
        if (StringUtils.hasText(configuredApiKey)) {
            return configuredApiKey;
        }
        String deepSeekApiKey = System.getenv("DEEPSEEK_API_KEY");
        if (StringUtils.hasText(deepSeekApiKey)) {
            return deepSeekApiKey;
        }
        String deepSeekApiKeyCompact = System.getenv("DEEPSEEK_APIKEY");
        if (StringUtils.hasText(deepSeekApiKeyCompact)) {
            return deepSeekApiKeyCompact;
        }
        return System.getenv("OPENAI_API_KEY");
    }
}
