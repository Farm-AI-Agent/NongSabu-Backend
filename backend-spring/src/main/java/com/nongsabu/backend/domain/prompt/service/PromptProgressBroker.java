package com.nongsabu.backend.domain.prompt.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.nongsabu.backend.domain.prompt.dto.PromptProgressEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class PromptProgressBroker {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, PromptProgressEvent> latestEvents = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String sessionId) {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        emitters.computeIfAbsent(sessionId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        PromptProgressEvent latest = latestEvents.get(sessionId);
        if (latest != null) {
            send(emitter, latest);
        }
        return emitter;
    }

    public void publish(String sessionId, String status, String message) {
        PromptProgressEvent event = new PromptProgressEvent(sessionId, status, message, LocalDateTime.now());
        latestEvents.put(sessionId, event);
        emitters.getOrDefault(sessionId, List.of()).forEach(emitter -> send(emitter, event));
    }

    public void publishChunk(String sessionId, String chunk) {
        PromptProgressEvent event = new PromptProgressEvent(sessionId, "streaming", chunk, LocalDateTime.now());
        emitters.getOrDefault(sessionId, List.of()).forEach(emitter -> sendChunk(emitter, chunk));
    }

    public void complete(String sessionId) {
        publish(sessionId, "completed", "응답 생성 완료");
    }

    private void send(SseEmitter emitter, PromptProgressEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("prompt-progress")
                    .data(event));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event()
                    .name("prompt-chunk")
                    .data(chunk));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {
        emitters.getOrDefault(sessionId, List.of()).remove(emitter);
    }
}
