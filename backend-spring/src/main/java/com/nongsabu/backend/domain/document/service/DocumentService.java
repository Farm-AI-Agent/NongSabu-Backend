package com.nongsabu.backend.domain.document.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.dto.DocumentSummaryResponse;
import com.nongsabu.backend.domain.document.dto.DocumentUploadResponse;
import com.nongsabu.backend.domain.document.entity.DocumentAsset;
import com.nongsabu.backend.domain.document.entity.DocumentChunk;
import com.nongsabu.backend.domain.document.entity.DocumentParsingStatus;
import com.nongsabu.backend.domain.document.repository.DocumentAssetRepository;
import com.nongsabu.backend.domain.document.repository.DocumentChunkRepository;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.service.MemberService;
import com.nongsabu.backend.infra.search.opensearch.Bm25SearchClient;
import com.nongsabu.backend.infra.search.opensearch.OpenSearchIndexingException;
import com.nongsabu.backend.infra.storage.LocalStorageService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private static final String SCOPE_MEMBER = "MEMBER";
    private static final String SCOPE_GLOBAL = "GLOBAL";

    private final DocumentAssetRepository documentAssetRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final MemberService memberService;
    private final LocalStorageService localStorageService;
    private final DocumentParser documentParser;
    private final DocumentChunker documentChunker;
    private final VectorStore vectorStore;
    private final Bm25SearchClient bm25SearchClient;

    @Value("${app.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    @Transactional
    public DocumentUploadResponse upload(Long memberId, MultipartFile file) {
        return upload(memberId, file, false);
    }

    @Transactional
    public DocumentUploadResponse uploadGlobal(Long ownerMemberId, MultipartFile file) {
        return upload(ownerMemberId, file, true);
    }

    private DocumentUploadResponse upload(Long memberId, MultipartFile file, boolean globalScope) {
        Member member = memberService.getMember(memberId);
        List<String> chunks = documentChunker.chunk(documentParser.parsePdf(file));
        DocumentAsset asset = createAsset(member, file, globalScope);

        documentChunkRepository.saveAll(toDocumentChunks(asset, chunks));
        vectorStore.add(toVectorDocuments(asset, memberId, chunks, globalScope));
        indexOpenSearch(asset, memberId, chunks, globalScope);
        asset.updateParsingStatus(DocumentParsingStatus.PARSED);

        return DocumentUploadResponse.from(asset, chunks.size(), embeddingModel);
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> getDocuments(Long memberId) {
        return documentAssetRepository.findAllByMemberId(memberId).stream()
                .map(DocumentSummaryResponse::from)
                .toList();
    }

    private DocumentAsset createAsset(Member member, MultipartFile file, boolean globalScope) {
        try {
            String storedPath = localStorageService.store("documents", file);
            return documentAssetRepository.save(DocumentAsset.builder()
                    .member(member)
                    .originalFilename(file.getOriginalFilename() == null ? "unknown.pdf" : file.getOriginalFilename())
                    .storagePath(storedPath)
                    .contentType("application/pdf")
                    .sourceType(globalScope ? "admin-global-upload" : "manual-upload")
                    .parsingStatus(DocumentParsingStatus.UPLOADED)
                    .build());
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "문서 파일 저장에 실패했습니다.");
        }
    }

    private List<Document> toVectorDocuments(DocumentAsset asset, Long memberId, List<String> chunks, boolean globalScope) {
        return IntStream.range(0, chunks.size())
                .mapToObj(index -> new Document(
                        chunks.get(index),
                        Map.of(
                                "memberId", String.valueOf(memberId),
                                "scope", globalScope ? SCOPE_GLOBAL : SCOPE_MEMBER,
                                "documentId", String.valueOf(asset.getId()),
                                "chunkIndex", index,
                                "filename", asset.getOriginalFilename()
                        )
                ))
                .toList();
    }

    private List<DocumentChunk> toDocumentChunks(DocumentAsset asset, List<String> chunks) {
        return IntStream.range(0, chunks.size())
                .mapToObj(index -> DocumentChunk.builder()
                        .documentAsset(asset)
                        .chunkIndex(index)
                        .content(chunks.get(index))
                        .build())
                .toList();
    }

    private void indexOpenSearch(DocumentAsset asset, Long memberId, List<String> chunks, boolean globalScope) {
        try {
            bm25SearchClient.indexChunks(asset, memberId, chunks, globalScope ? SCOPE_GLOBAL : SCOPE_MEMBER);
            asset.markOpenSearchIndexed();
        } catch (OpenSearchIndexingException exception) {
            log.warn("OpenSearch indexing failed. documentId={}, filename={}",
                    asset.getId(), asset.getOriginalFilename(), exception);
            asset.markOpenSearchIndexFailed(exception.getMessage());
        }
    }
}
