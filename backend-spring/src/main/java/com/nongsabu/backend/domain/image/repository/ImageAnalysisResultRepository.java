package com.nongsabu.backend.domain.image.repository;

import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageAnalysisResultRepository extends JpaRepository<ImageAnalysisResult, Long> {

    Optional<ImageAnalysisResult> findByUploadedImageId(Long uploadedImageId);

    List<ImageAnalysisResult> findAllByUploadedImageIdIn(Collection<Long> uploadedImageIds);
}
