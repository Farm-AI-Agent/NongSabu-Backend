package com.nongsabu.backend.domain.usercrop.repository;

import java.util.List;
import com.nongsabu.backend.domain.usercrop.entity.UserCrop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCropRepository extends JpaRepository<UserCrop, Long> {

    List<UserCrop> findAllByMemberId(Long memberId);
}

