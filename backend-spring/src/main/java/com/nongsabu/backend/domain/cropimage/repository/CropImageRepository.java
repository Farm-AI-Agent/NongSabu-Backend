package com.nongsabu.backend.domain.cropimage.repository;

import java.util.List;
import com.nongsabu.backend.domain.cropimage.entity.CropImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropImageRepository extends JpaRepository<CropImage, Long> {

    List<CropImage> findAllByMemberId(Long memberId);
}

