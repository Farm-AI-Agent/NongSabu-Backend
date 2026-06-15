package com.nongsabu.backend.domain.image.repository;

import java.util.Optional;
import com.nongsabu.backend.domain.image.entity.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {

    Optional<UploadedImage> findByIdAndMemberId(Long id, Long memberId);
}
