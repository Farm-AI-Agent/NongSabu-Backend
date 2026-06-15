package com.nongsabu.backend.domain.image.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.nongsabu.backend.domain.image.dto.AnalysisProgressEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class AnalysisProgressBroker {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<Long, AnalysisProgressEvent> latestEvents = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long imageId) {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        emitters.computeIfAbsent(imageId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(imageId, emitter));
        emitter.onTimeout(() -> removeEmitter(imageId, emitter));
        AnalysisProgressEvent latest = latestEvents.get(imageId);
        if (latest != null) {
            send(emitter, latest);
        }
        return emitter;
    }

    public void publish(Long imageId, String status, String message) {
        AnalysisProgressEvent event = new AnalysisProgressEvent(imageId, status, message, LocalDateTime.now());
        latestEvents.put(imageId, event);
        emitters.getOrDefault(imageId, List.of()).forEach(emitter -> send(emitter, event));
    }

    private void send(SseEmitter emitter, AnalysisProgressEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("analysis-progress")
                    .data(event));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(Long imageId, SseEmitter emitter) {
        emitters.getOrDefault(imageId, List.of()).remove(emitter);
    }
}

