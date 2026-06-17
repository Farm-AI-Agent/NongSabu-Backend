package com.nongsabu.backend.domain.chat.service;

import com.nongsabu.backend.domain.chat.dto.ChatRequest;
import com.nongsabu.backend.domain.chat.dto.ChatResponse;
import com.nongsabu.backend.domain.document.dto.RagAnswerResponse;
import com.nongsabu.backend.domain.document.service.RagService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiseaseChatService {

    private final RagService ragService;

    public ChatResponse ask(Long memberId, ChatRequest request) {
        RagAnswerResponse ragResponse = ragService.ask(memberId, request.question(), 4, "hybrid-rerank");
        boolean hasContext = !ragResponse.sources().isEmpty();
        List<String> sourceLabels = ragResponse.sources().stream()
                .map(item -> item.filename() + " (청크 " + item.chunkIndex() + ")")
                .distinct()
                .toList();
        return new ChatResponse(
                request.question(),
                ragResponse.answer(),
                "rag-hybrid-rerank",
                hasContext,
                sourceLabels
        );
    }
}
