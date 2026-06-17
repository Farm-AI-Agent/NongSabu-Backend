package com.nongsabu.backend.domain.externalapilog.entity;

import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.report.entity.AnalysisReport;
import java.time.LocalDateTime;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Builder
@Entity
@Table(name = "external_api_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExternalApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부 API 호출이 어떤 회원 요청에서 발생했는지 추적하기 위한 선택 연결이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 리포트 생성 과정에서 사용된 외부 API 응답을 나중에 역추적할 수 있게 연결한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_report_id")
    private AnalysisReport analysisReport;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "request_params", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String requestParams;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(nullable = false)
    private boolean success;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

