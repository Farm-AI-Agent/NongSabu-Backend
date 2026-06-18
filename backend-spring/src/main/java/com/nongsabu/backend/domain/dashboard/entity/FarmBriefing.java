package com.nongsabu.backend.domain.dashboard.entity;

import com.nongsabu.backend.common.entity.BaseEntity;
import com.nongsabu.backend.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "farm_briefing")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FarmBriefing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "briefing_date", nullable = false)
    private LocalDate briefingDate;

    @Column(name = "weather_hash", nullable = false, length = 64)
    private String weatherHash;

    @Column(name = "profile_hash", nullable = false, length = 64)
    private String profileHash;

    @Column(name = "weather_payload", nullable = false, columnDefinition = "text")
    private String weatherPayload;

    @Column(name = "response_payload", nullable = false, columnDefinition = "text")
    private String responsePayload;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;
}
