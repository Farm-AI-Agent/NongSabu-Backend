package com.nongsabu.backend.domain.farm.repository;

import java.util.List;
import java.util.Optional;
import com.nongsabu.backend.domain.farm.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmRepository extends JpaRepository<Farm, Long> {

    List<Farm> findAllByMemberId(Long memberId);

    Optional<Farm> findByIdAndMemberId(Long id, Long memberId);

    Optional<Farm> findFirstByMemberIdOrderByIdAsc(Long memberId);
}
