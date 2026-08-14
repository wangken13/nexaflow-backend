package com.vebcoding.trade.ai.service;

import com.vebcoding.trade.ai.api.AiProviderStatus;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

@Component
@Primary
public class SpringAiProvider implements AiProviderStrategy {
    private static final Logger log = LoggerFactory.getLogger(SpringAiProvider.class);

    private final LocalFallbackAiProvider fallbackAiProvider;
    private final ChatClient chatClient;
    private final String model;
    private final boolean configured;

    public SpringAiProvider(
            LocalFallbackAiProvider fallbackAiProvider,
            @Value("${spring.ai.openai.api-key:}") String configuredApiKey,
            @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model:deepseek-chat}") String model) {
        this.fallbackAiProvider = fallbackAiProvider;
        this.model = model;
        String apiKey = configuredApiKey == null ? "" : configuredApiKey.trim();
        this.configured = StringUtils.hasText(apiKey);
        this.chatClient = configured ? createChatClient(apiKey, baseUrl, model) : null;
    }

    @Override
    public String generate(String prompt) {
        if (!configured) {
            return fallbackAiProvider.generate(prompt);
        }

        long startedAt = System.nanoTime();
        try {
            String content = chatClient.prompt().user(prompt).call().content();
            if (!StringUtils.hasText(content)) {
                log.warn("AI model returned empty content, model={}", model);
                return fallbackAiProvider.generate(prompt);
            }
            log.info("AI analysis completed, model={}, promptChars={}, elapsedMs={}", model, prompt.length(), elapsedMillis(startedAt));
            return content;
        } catch (RuntimeException ex) {
            log.warn("AI analysis degraded, model={}, errorType={}, elapsedMs={}",
                    model, ex.getClass().getSimpleName(), elapsedMillis(startedAt));
            return fallbackAiProvider.generate(prompt);
        }
    }

    @Override
    public Flux<String> stream(String prompt) {
        if (!configured) {
            return fallbackAiProvider.stream(prompt);
        }

        return Flux.defer(() -> {
            long startedAt = System.nanoTime();
            return chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .filter(StringUtils::hasText)
                    .switchIfEmpty(fallbackAiProvider.stream(prompt))
                    .doOnComplete(() -> log.info(
                            "AI streaming analysis completed, model={}, promptChars={}, elapsedMs={}",
                            model, prompt.length(), elapsedMillis(startedAt)))
                    .onErrorResume(ex -> {
                        log.warn("AI streaming analysis degraded, model={}, errorType={}, elapsedMs={}",
                                model, ex.getClass().getSimpleName(), elapsedMillis(startedAt));
                        return fallbackAiProvider.stream(prompt);
                    });
        });
    }

    @Override
    public AiProviderStatus status() {
        return new AiProviderStatus("DeepSeek", model, configured, true);
    }

    private ChatClient createChatClient(String apiKey, String baseUrl, String model) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .completionsPath("/chat/completions")
                .restClientBuilder(modelRestClient())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.3)
                .maxTokens(1_200)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
        return ChatClient.builder(chatModel)
                .defaultSystem("你是 NexaFlow 企业客户协同平台的 AI 业务助手。回答必须准确、克制，不得编造客户未提供的信息。")
                .build();
    }

    private RestClient.Builder modelRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().requestFactory(requestFactory);
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
