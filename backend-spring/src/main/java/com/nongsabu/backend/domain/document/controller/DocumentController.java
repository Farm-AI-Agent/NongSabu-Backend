package com.nongsabu.backend.domain.document.controller;

import java.util.List;
import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.document.dto.DocumentSummaryResponse;
import com.nongsabu.backend.domain.document.dto.DocumentUploadResponse;
import com.nongsabu.backend.domain.document.dto.RagSearchRequest;
import com.nongsabu.backend.domain.document.dto.RagSearchResponse;
import com.nongsabu.backend.domain.document.service.DocumentService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/api/v1/documents")
    public ApiResponse<DocumentUploadResponse> upload(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam MultipartFile file
    ) {
        return ApiResponse.ok("문서 업로드 및 인덱싱이 완료되었습니다.", documentService.upload(principal.id(), file));
    }

    @GetMapping("/api/v1/documents")
    public ApiResponse<List<DocumentSummaryResponse>> getDocuments(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok("문서 목록 조회에 성공했습니다.", documentService.getDocuments(principal.id()));
    }

    @PostMapping("/api/v1/rag/search")
    public ApiResponse<RagSearchResponse> search(@Valid @RequestBody RagSearchRequest request) {
        return ApiResponse.ok("RAG 검색에 성공했습니다.", documentService.search(request.query(), request.topK()));
    }
}
