package com.nongsabu.backend.domain.dashboard.repository;

import com.nongsabu.backend.domain.dashboard.entity.FarmBriefing;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmBriefingRepository extends JpaRepository<FarmBriefing, Long> {

    Optional<FarmBriefing> findFirstByMemberIdAndBriefingDateAndWeatherHashAndProfileHashOrderByCreatedAtDesc(
            Long memberId,
            LocalDate briefingDate,
            String weatherHash,
            String profileHash
    );
}
