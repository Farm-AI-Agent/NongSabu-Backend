package com.nongsabu.backend.domain.prompt.controller;

import com.nongsabu.backend.domain.prompt.dto.PromptRequest;
import com.nongsabu.backend.domain.prompt.service.PromptProcessingService;
import com.nongsabu.backend.domain.prompt.service.PromptProgressBroker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptProcessingService promptProcessingService;
    private final PromptProgressBroker progressBroker;

    @PostMapping("/ask")
    public void askQuestion(@RequestBody PromptRequest request) {
        promptProcessingService.processPrompt(request.sessionId(), request.question());
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String sessionId) {
        return progressBroker.subscribe(sessionId);
    }
}
