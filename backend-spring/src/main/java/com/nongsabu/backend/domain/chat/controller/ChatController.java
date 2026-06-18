package com.nongsabu.backend.domain.chat.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.chat.dto.ChatRequest;
import com.nongsabu.backend.domain.chat.dto.ChatResponse;
import com.nongsabu.backend.domain.chat.service.DiseaseChatService;
import com.nongsabu.backend.domain.chat.service.GeneralChatService;
import com.nongsabu.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final DiseaseChatService diseaseChatService;
    private final GeneralChatService generalChatService;

    @PostMapping("/disease")
    public ApiResponse<ChatResponse> askDisease(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ChatRequest request
    ) {
        return ApiResponse.ok("병충해 판단 답변 완료.", diseaseChatService.ask(principal.id(), request));
    }

    @PostMapping("/general")
    public ApiResponse<ChatResponse> askGeneral(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ChatRequest request
    ) {
        return ApiResponse.ok("농사지식 답변 완료.", generalChatService.ask(principal.id(), request));
    }
}
