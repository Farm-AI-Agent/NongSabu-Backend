package com.nongsabu.backend.domain.crop.repository;

import com.nongsabu.backend.domain.crop.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropRepository extends JpaRepository<Crop, Long> {
}

