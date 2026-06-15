package com.nongsabu.backend.domain.document.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.dto.DocumentSummaryResponse;
import com.nongsabu.backend.domain.document.dto.DocumentUploadResponse;
import com.nongsabu.backend.domain.document.dto.RagSearchItem;
import com.nongsabu.backend.domain.document.dto.RagSearchResponse;
import com.nongsabu.backend.domain.document.entity.DocumentAsset;
import com.nongsabu.backend.domain.document.entity.DocumentChunk;
import com.nongsabu.backend.domain.document.entity.DocumentParsingStatus;
import com.nongsabu.backend.domain.document.repository.DocumentAssetRepository;
import com.nongsabu.backend.domain.document.repository.DocumentChunkRepository;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.service.MemberService;
import com.nongsabu.backend.infra.storage.LocalStorageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentAssetRepository documentAssetRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final MemberService memberService;
    private final LocalStorageService localStorageService;
    private final EmbeddingService embeddingService;

    @Transactional
    public DocumentUploadResponse upload(Long memberId, MultipartFile file) {
        Member member = memberService.getMember(memberId);
        DocumentAsset asset = createAsset(member, file);
        try {
            String parsedText = parse(file);
            List<String> chunks = chunk(parsedText, 400, 80);
            for (int i = 0; i < chunks.size(); i++) {
                documentChunkRepository.save(DocumentChunk.builder()
                        .documentAsset(asset)
                        .chunkIndex(i)
                        .content(chunks.get(i))
                        .embedding(embeddingService.embed(chunks.get(i)))
                        .embeddingModel("dummy-char-embedding-v1")
                        .build());
            }
            asset.updateParsingStatus(DocumentParsingStatus.PARSED);
        } catch (Exception exception) {
            asset.updateParsingStatus(DocumentParsingStatus.FAILED);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "문서 파싱에 실패했습니다: " + exception.getMessage());
        }
        return DocumentUploadResponse.from(asset);
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> getDocuments(Long memberId) {
        return documentAssetRepository.findAllByMemberId(memberId).stream()
                .map(DocumentSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RagSearchResponse search(String query, int topK) {
        String embedding = embeddingService.embed(query);
        List<RagSearchItem> items = documentChunkRepository.searchByEmbedding(embedding, topK).stream()
                .map(RagSearchItem::from)
                .toList();
        return new RagSearchResponse(query, items);
    }

    @Transactional(readOnly = true)
    public List<String> getContextSnippets(String query, int topK) {
        return search(query, topK).items().stream()
                .map(RagSearchItem::content)
                .toList();
    }

    private DocumentAsset createAsset(Member member, MultipartFile file) {
        try {
            String storedPath = localStorageService.store("documents", file);
            return documentAssetRepository.save(DocumentAsset.builder()
                    .member(member)
                    .originalFilename(file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename())
                    .storagePath(storedPath)
                    .contentType(file.getContentType())
                    .sourceType("manual-upload")
                    .parsingStatus(DocumentParsingStatus.UPLOADED)
                    .build());
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "문서 저장에 실패했습니다.");
        }
    }

    private String parse(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private List<String> chunk(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            chunks.add("문서 내용이 비어 있습니다.");
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + size);
            chunks.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }
}
