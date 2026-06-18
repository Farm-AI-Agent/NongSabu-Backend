package com.nongsabu.backend.domain.document.service;

import static com.nongsabu.backend.support.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.dto.DocumentSummaryResponse;
import com.nongsabu.backend.domain.document.dto.DocumentUploadResponse;
import com.nongsabu.backend.domain.document.entity.DocumentAsset;
import com.nongsabu.backend.domain.document.entity.DocumentChunk;
import com.nongsabu.backend.domain.document.entity.DocumentParsingStatus;
import com.nongsabu.backend.domain.document.repository.DocumentAssetRepository;
import com.nongsabu.backend.domain.document.repository.DocumentChunkRepository;
import com.nongsabu.backend.domain.member.service.MemberService;
import com.nongsabu.backend.infra.search.opensearch.Bm25SearchClient;
import com.nongsabu.backend.infra.storage.LocalStorageService;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentAssetRepository documentAssetRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private LocalStorageService localStorageService;

    @Mock
    private DocumentParser documentParser;

    @Mock
    private DocumentChunker documentChunker;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private Bm25SearchClient bm25SearchClient;

    @InjectMocks
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "embeddingModel", "test-embedding-model");
    }

    @Test
    void uploadParsesChunksStoresAssetAndAddsVectorDocuments() throws IOException {
        var member = member(1L);
        MockMultipartFile file = pdfFile();
        DocumentAsset saved = documentAsset(10L, member, DocumentParsingStatus.UPLOADED);
        ArgumentCaptor<List<Document>> vectorDocuments = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<DocumentChunk>> documentChunks = ArgumentCaptor.forClass(List.class);

        given(memberService.getMember(1L)).willReturn(member);
        given(documentParser.parsePdf(file)).willReturn("manual text");
        given(documentChunker.chunk("manual text")).willReturn(List.of("chunk-1", "chunk-2"));
        given(localStorageService.store(eq("documents"), eq(file))).willReturn("/uploads/documents/manual.pdf");
        given(documentAssetRepository.save(any(DocumentAsset.class))).willReturn(saved);

        DocumentUploadResponse response = documentService.upload(1L, file);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.chunkCount()).isEqualTo(2);
        assertThat(response.embeddingModel()).isEqualTo("test-embedding-model");
        assertThat(response.parsingStatus()).isEqualTo(DocumentParsingStatus.PARSED.name());
        verify(documentChunkRepository).saveAll(documentChunks.capture());
        assertThat(documentChunks.getValue()).hasSize(2);
        assertThat(documentChunks.getValue().get(0).getChunkIndex()).isZero();
        assertThat(documentChunks.getValue().get(0).getContent()).isEqualTo("chunk-1");
        verify(vectorStore).add(vectorDocuments.capture());
        assertThat(vectorDocuments.getValue()).hasSize(2);
        assertThat(vectorDocuments.getValue().get(0).getMetadata())
                .containsEntry("memberId", "1")
                .containsEntry("scope", "MEMBER")
                .containsEntry("documentId", "10")
                .containsEntry("chunkIndex", 0);
        verify(bm25SearchClient).indexChunks(saved, 1L, List.of("chunk-1", "chunk-2"), "MEMBER");
    }

    @Test
    void uploadGlobalAddsGlobalScopeToVectorAndSearchIndexes() throws IOException {
        var member = member(1L);
        MockMultipartFile file = pdfFile();
        DocumentAsset saved = documentAsset(20L, member, DocumentParsingStatus.UPLOADED);
        ArgumentCaptor<DocumentAsset> asset = ArgumentCaptor.forClass(DocumentAsset.class);
        ArgumentCaptor<List<Document>> vectorDocuments = ArgumentCaptor.forClass(List.class);

        given(memberService.getMember(1L)).willReturn(member);
        given(documentParser.parsePdf(file)).willReturn("global manual text");
        given(documentChunker.chunk("global manual text")).willReturn(List.of("global-chunk"));
        given(localStorageService.store(eq("documents"), eq(file))).willReturn("/uploads/documents/manual.pdf");
        given(documentAssetRepository.save(any(DocumentAsset.class))).willReturn(saved);

        DocumentUploadResponse response = documentService.uploadGlobal(1L, file);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.chunkCount()).isEqualTo(1);
        verify(documentAssetRepository).save(asset.capture());
        assertThat(asset.getValue().getSourceType()).isEqualTo("admin-global-upload");
        verify(vectorStore).add(vectorDocuments.capture());
        assertThat(vectorDocuments.getValue()).hasSize(1);
        assertThat(vectorDocuments.getValue().get(0).getMetadata())
                .containsEntry("memberId", "1")
                .containsEntry("scope", "GLOBAL")
                .containsEntry("documentId", "20")
                .containsEntry("chunkIndex", 0);
        verify(bm25SearchClient).indexChunks(saved, 1L, List.of("global-chunk"), "GLOBAL");
    }

    @Test
    void uploadFailsWhenStorageFails() throws IOException {
        MockMultipartFile file = pdfFile();
        given(memberService.getMember(1L)).willReturn(member(1L));
        given(documentParser.parsePdf(file)).willReturn("manual text");
        given(documentChunker.chunk("manual text")).willReturn(List.of("chunk-1"));
        given(localStorageService.store(eq("documents"), eq(file))).willThrow(new IOException("disk full"));

        assertThatThrownBy(() -> documentService.upload(1L, file))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getDocumentsReturnsOnlyMemberDocumentsFromRepositoryQuery() {
        var member = member(1L);
        given(documentAssetRepository.findAllByMemberId(1L))
                .willReturn(List.of(documentAsset(10L, member, DocumentParsingStatus.PARSED)));

        List<DocumentSummaryResponse> response = documentService.getDocuments(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(10L);
        assertThat(response.get(0).parsingStatus()).isEqualTo(DocumentParsingStatus.PARSED.name());
    }

    private MockMultipartFile pdfFile() {
        return new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "pdf".getBytes()
        );
    }

    private DocumentAsset documentAsset(Long id, com.nongsabu.backend.domain.member.entity.Member member, DocumentParsingStatus status) {
        return DocumentAsset.builder()
                .id(id)
                .member(member)
                .originalFilename("manual.pdf")
                .storagePath("/uploads/documents/manual.pdf")
                .contentType("application/pdf")
                .sourceType("manual-upload")
                .parsingStatus(status)
                .build();
    }
}
