package com.nongsabu.backend.domain.crop.repository;

import java.util.List;
import com.nongsabu.backend.domain.crop.entity.FarmCrop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmCropRepository extends JpaRepository<FarmCrop, Long> {

    boolean existsByFarmIdAndCropId(Long farmId, Long cropId);

    List<FarmCrop> findAllByFarmId(Long farmId);
}

