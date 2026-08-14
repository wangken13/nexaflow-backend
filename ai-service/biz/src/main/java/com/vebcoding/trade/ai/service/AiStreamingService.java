package com.vebcoding.trade.ai.service;

import com.vebcoding.trade.ai.api.AiStreamEvent;
import com.vebcoding.trade.common.TenantContext;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

@Service
public class AiStreamingService {
    private static final Logger log = LoggerFactory.getLogger(AiStreamingService.class);
    private static final long STREAM_TIMEOUT_MILLIS = 75_000L;

    private final AiService aiService;

    public AiStreamingService(AiService aiService) {
        this.aiService = aiService;
    }

    public SseEmitter analyzeInquiry(String inquiryId, String content) {
        AiService.AnalysisPlan plan = aiService.prepareAnalysis(content);
        SecurityContextSnapshot context = new SecurityContextSnapshot(
                TenantContext.tenantId(), TenantContext.userId(), TenantContext.role());
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        StringBuilder generatedContent = new StringBuilder();
        AtomicLong sequence = new AtomicLong();
        AtomicBoolean terminated = new AtomicBoolean();
        AtomicReference<Disposable> subscription = new AtomicReference<>();

        emitter.onTimeout(() -> terminate(subscription, terminated));
        emitter.onCompletion(() -> terminate(subscription, terminated));
        emitter.onError(error -> terminate(subscription, terminated));

        Disposable disposable = aiService.streamAnalysis(plan).subscribe(
                chunk -> {
                    if (terminated.get()) return;
                    generatedContent.append(chunk);
                    send(emitter, sequence, "delta", AiStreamEvent.delta(chunk), subscription, terminated);
                },
                error -> {
                    if (!terminated.compareAndSet(false, true)) return;
                    log.warn("AI SSE stream failed, inquiryId={}, errorType={}",
                            inquiryId, error.getClass().getSimpleName());
                    try {
                        emitter.send(SseEmitter.event().name("error")
                                .data(AiStreamEvent.failed("AI 分析暂时不可用，请稍后重试")));
                    } catch (IOException ignored) {
                        // The client may already have closed the connection.
                    }
                    emitter.complete();
                },
                () -> complete(emitter, inquiryId, plan, generatedContent.toString(), context,
                        sequence, subscription, terminated));
        subscription.set(disposable);
        return emitter;
    }

    private void complete(SseEmitter emitter, String inquiryId, AiService.AnalysisPlan plan,
                          String generatedContent, SecurityContextSnapshot context,
                          AtomicLong sequence, AtomicReference<Disposable> subscription,
                          AtomicBoolean terminated) {
        if (!terminated.compareAndSet(false, true)) return;
        try {
            context.restore();
            var analysis = aiService.completeAnalysis(inquiryId, plan, generatedContent);
            emitter.send(SseEmitter.event().id(String.valueOf(sequence.incrementAndGet()))
                    .name("complete").data(AiStreamEvent.completed(analysis)));
            emitter.complete();
        } catch (Exception error) {
            log.warn("AI SSE result persistence failed, inquiryId={}, errorType={}",
                    inquiryId, error.getClass().getSimpleName());
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data(AiStreamEvent.failed("AI 分析结果保存失败，请重试")));
            } catch (IOException ignored) {
                // The client may already have closed the connection.
            }
            emitter.complete();
        } finally {
            TenantContext.clear();
            Disposable current = subscription.get();
            if (current != null && !current.isDisposed()) current.dispose();
        }
    }

    private void send(SseEmitter emitter, AtomicLong sequence, String eventName, AiStreamEvent event,
                      AtomicReference<Disposable> subscription, AtomicBoolean terminated) {
        try {
            emitter.send(SseEmitter.event().id(String.valueOf(sequence.incrementAndGet()))
                    .name(eventName).data(event));
        } catch (IOException error) {
            terminate(subscription, terminated);
        }
    }

    private void terminate(AtomicReference<Disposable> subscription, AtomicBoolean terminated) {
        terminated.set(true);
        Disposable current = subscription.get();
        if (current != null && !current.isDisposed()) current.dispose();
    }

    private record SecurityContextSnapshot(String tenantId, String userId, String role) {
        void restore() {
            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(userId);
            TenantContext.setRole(role);
        }
    }
}
