package com.nongsabu.backend.domain.document.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(
        name = "document_chunks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_chunks_asset_index",
                columnNames = {"document_asset_id", "chunk_index"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_asset_id", nullable = false)
    private DocumentAsset documentAsset;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    // embedding 컬럼은 pgvector에 존재하지만, 현재 MVP에서는 Spring AI VectorStore가 실제 벡터 저장을 담당한다.
    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "section_title", length = 255)
    private String sectionTitle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
