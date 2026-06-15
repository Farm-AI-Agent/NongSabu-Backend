package com.nongsabu.backend.domain.image.repository;

import java.util.Optional;
import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageAnalysisResultRepository extends JpaRepository<ImageAnalysisResult, Long> {

    Optional<ImageAnalysisResult> findByUploadedImageId(Long uploadedImageId);
}

