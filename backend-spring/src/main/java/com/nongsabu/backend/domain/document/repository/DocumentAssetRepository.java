package com.nongsabu.backend.domain.document.repository;

import java.util.List;
import com.nongsabu.backend.domain.document.entity.DocumentAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAssetRepository extends JpaRepository<DocumentAsset, Long> {

    List<DocumentAsset> findAllByMemberId(Long memberId);
}
