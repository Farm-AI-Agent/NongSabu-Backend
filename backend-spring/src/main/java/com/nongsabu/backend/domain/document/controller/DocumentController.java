package com.nongsabu.backend.domain.document.controller;

import com.nongsabu.backend.common.api.ApiResponse;
import com.nongsabu.backend.domain.document.dto.DocumentSummaryResponse;
import com.nongsabu.backend.domain.document.dto.DocumentUploadResponse;
import com.nongsabu.backend.domain.document.dto.OpenSearchReindexResponse;
import com.nongsabu.backend.domain.document.dto.RagAnswerResponse;
import com.nongsabu.backend.domain.document.dto.RagAskRequest;
import com.nongsabu.backend.domain.document.dto.RagSearchRequest;
import com.nongsabu.backend.domain.document.dto.RagSearchResponse;
import com.nongsabu.backend.domain.document.service.DocumentService;
import com.nongsabu.backend.domain.document.service.OpenSearchReindexService;
import com.nongsabu.backend.domain.document.service.RagService;
import com.nongsabu.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final RagService ragService;
    private final OpenSearchReindexService openSearchReindexService;

    @PostMapping(value = "/api/v1/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentUploadResponse> upload(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(
                "PDF 업로드와 벡터 인덱싱이 완료되었습니다.",
                documentService.upload(principal.id(), file)
        );
    }

    @GetMapping("/api/v1/documents")
    public ApiResponse<List<DocumentSummaryResponse>> getDocuments(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ApiResponse.ok("문서 목록 조회에 성공했습니다.", documentService.getDocuments(principal.id()));
    }

    @PostMapping("/api/v1/rag/search")
    public ApiResponse<RagSearchResponse> search(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody RagSearchRequest request
    ) {
        return ApiResponse.ok(
                "RAG 검색에 성공했습니다.",
                ragService.search(principal.id(), request.query(), request.topK(), request.retrievalMode())
        );
    }

    @PostMapping("/api/v1/rag/ask")
    public ApiResponse<RagAnswerResponse> ask(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody RagAskRequest request
    ) {
        return ApiResponse.ok(
                "RAG 답변 생성에 성공했습니다.",
                ragService.ask(principal.id(), request.question(), request.topK(), request.retrievalMode())
        );
    }

    @PostMapping("/api/v1/rag/opensearch/reindex")
    public ApiResponse<OpenSearchReindexResponse> reindexOpenSearch(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ApiResponse.ok(
                "OpenSearch reindex completed.",
                openSearchReindexService.reindexCurrentUser(principal.id())
        );
    }
}
