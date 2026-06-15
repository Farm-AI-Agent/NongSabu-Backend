package com.nongsabu.backend.domain.externalapilog.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "request_params", columnDefinition = "jsonb")
    private String requestParams;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(nullable = false)
    private boolean success;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

