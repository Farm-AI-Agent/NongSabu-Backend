package com.nongsabu.backend.domain.document.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.dto.DocumentSummaryResponse;
import com.nongsabu.backend.domain.document.dto.DocumentUploadResponse;
import com.nongsabu.backend.domain.document.entity.DocumentAsset;
import com.nongsabu.backend.domain.document.entity.DocumentParsingStatus;
import com.nongsabu.backend.domain.document.repository.DocumentAssetRepository;
import com.nongsabu.backend.domain.user.entity.User;
import com.nongsabu.backend.domain.user.service.UserService;
import com.nongsabu.backend.infra.storage.LocalStorageService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentAssetRepository documentAssetRepository;
    private final UserService userService;
    private final LocalStorageService localStorageService;
    private final DocumentParser documentParser;
    private final DocumentChunker documentChunker;
    private final VectorStore vectorStore;

    @Value("${app.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    @Transactional
    public DocumentUploadResponse upload(Long userId, MultipartFile file) {
        User user = userService.getUser(userId);
        List<String> chunks = documentChunker.chunk(documentParser.parsePdf(file));
        DocumentAsset asset = createAsset(user, file);

        vectorStore.add(toVectorDocuments(asset, userId, chunks));
        asset.updateParsingStatus(DocumentParsingStatus.PARSED);

        return DocumentUploadResponse.from(asset, chunks.size(), embeddingModel);
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> getDocuments(Long userId) {
        return documentAssetRepository.findAllByUploadedById(userId).stream()
                .map(DocumentSummaryResponse::from)
                .toList();
    }

    private DocumentAsset createAsset(User user, MultipartFile file) {
        try {
            String storedPath = localStorageService.store("documents", file);
            return documentAssetRepository.save(DocumentAsset.builder()
                    .uploadedBy(user)
                    .originalFilename(file.getOriginalFilename() == null ? "unknown.pdf" : file.getOriginalFilename())
                    .storagePath(storedPath)
                    .contentType("application/pdf")
                    .sourceType("manual-upload")
                    .parsingStatus(DocumentParsingStatus.UPLOADED)
                    .build());
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "문서 파일 저장에 실패했습니다.");
        }
    }

    private List<Document> toVectorDocuments(DocumentAsset asset, Long userId, List<String> chunks) {
        return IntStream.range(0, chunks.size())
                .mapToObj(index -> new Document(
                        chunks.get(index),
                        Map.of(
                                "userId", String.valueOf(userId),
                                "documentId", String.valueOf(asset.getId()),
                                "chunkIndex", index,
                                "filename", asset.getOriginalFilename()
                        )
                ))
                .toList();
    }
}
