package com.nongsabu.backend.domain.agriculturedocument.entity;

import com.nongsabu.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Consider soft delete if document retention requirements are introduced.
@Getter
@Builder
@Entity
@Table(name = "agriculture_document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AgricultureDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "storage_path", length = 500)
    private String storagePath;
}

