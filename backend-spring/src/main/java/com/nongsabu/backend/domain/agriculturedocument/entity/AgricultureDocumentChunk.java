package com.nongsabu.backend.domain.agriculturedocument.entity;

import com.nongsabu.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

@Getter
@Builder
@Entity
@Table(name = "agriculture_document_chunk")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AgricultureDocumentChunk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private AgricultureDocument document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "vector")
    @ColumnTransformer(write = "?::vector")
    private String embedding;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}

