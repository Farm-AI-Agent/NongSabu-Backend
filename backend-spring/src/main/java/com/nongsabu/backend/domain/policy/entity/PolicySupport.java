package com.nongsabu.backend.domain.policy.entity;

import com.nongsabu.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(
        name = "policy_supports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_policy_supports_source_external",
                columnNames = {"source", "external_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicySupport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "external_id", nullable = false, length = 120)
    private String externalId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "target_group", length = 1000)
    private String targetGroup;

    @Column(length = 100)
    private String region;

    @Column(length = 120)
    private String category;

    @Column(name = "application_period", length = 120)
    private String applicationPeriod;

    @Column(name = "apply_method", length = 1000)
    private String applyMethod;

    @Column(length = 200)
    private String department;

    @Column(length = 100)
    private String contact;

    @Column(name = "detail_url", length = 1000)
    private String detailUrl;

    @Column(name = "content_text", nullable = false, columnDefinition = "text")
    private String contentText;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "embedding_synced_at")
    private LocalDateTime embeddingSyncedAt;

    public boolean hasSameContentHash(String nextContentHash) {
        return contentHash != null && contentHash.equals(nextContentHash);
    }

    public void updateFrom(PolicySupport next) {
        this.title = next.title;
        this.summary = next.summary;
        this.targetGroup = next.targetGroup;
        this.region = next.region;
        this.category = next.category;
        this.applicationPeriod = next.applicationPeriod;
        this.applyMethod = next.applyMethod;
        this.department = next.department;
        this.contact = next.contact;
        this.detailUrl = next.detailUrl;
        this.contentText = next.contentText;
        this.contentHash = next.contentHash;
        this.embeddingSyncedAt = null;
    }

    public void markEmbeddingSynced() {
        this.embeddingSyncedAt = LocalDateTime.now();
    }
}
