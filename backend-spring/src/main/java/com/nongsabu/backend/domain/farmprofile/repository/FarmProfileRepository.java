package com.nongsabu.backend.domain.farmprofile.repository;

import java.util.Optional;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmProfileRepository extends JpaRepository<FarmProfile, Long> {

    Optional<FarmProfile> findByMemberId(Long memberId);
}

