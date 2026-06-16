package com.nongsabu.backend.domain.farm.entity;

import com.nongsabu.backend.common.entity.BaseTimeEntity;
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
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 120)
    private String location;

    @Column(length = 50)
    private String cultivationArea;

    // 농장별 실제 재배 작물은 FarmCrop에서 관리한다. 이 필드는 자유 메모 용도만 담당한다.
    @Column(length = 1000)
    private String notes;

    public void update(String name, String location, String cultivationArea, String notes) {
        this.name = name;
        this.location = location;
        this.cultivationArea = cultivationArea;
        this.notes = notes;
    }
}
