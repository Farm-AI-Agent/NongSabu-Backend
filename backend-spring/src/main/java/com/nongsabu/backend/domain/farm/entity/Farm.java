package com.nongsabu.backend.domain.farm.entity;

import com.nongsabu.backend.common.entity.BaseTimeEntity;
import com.nongsabu.backend.domain.user.entity.User;
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

@Getter
@Builder
@Entity
@Table(name = "farms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Farm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 120)
    private String location;

    @Column(length = 50)
    private String cultivationArea;

    @Column(length = 255)
    private String cropSummary;

    @Column(length = 1000)
    private String notes;

    public void update(String name, String location, String cultivationArea, String cropSummary, String notes) {
        this.name = name;
        this.location = location;
        this.cultivationArea = cultivationArea;
        this.cropSummary = cropSummary;
        this.notes = notes;
    }
}

