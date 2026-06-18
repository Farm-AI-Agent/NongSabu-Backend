package com.nongsabu.backend.domain.image.repository;

import com.nongsabu.backend.domain.image.entity.UploadedImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {

    Optional<UploadedImage> findByIdAndMemberId(Long id, Long memberId);

    List<UploadedImage> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);
}
