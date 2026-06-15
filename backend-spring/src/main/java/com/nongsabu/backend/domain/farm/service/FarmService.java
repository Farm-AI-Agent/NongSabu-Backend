package com.nongsabu.backend.domain.farm.service;

import java.util.List;
import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.farm.dto.FarmRequest;
import com.nongsabu.backend.domain.farm.dto.FarmResponse;
import com.nongsabu.backend.domain.farm.entity.Farm;
import com.nongsabu.backend.domain.farm.repository.FarmRepository;
import com.nongsabu.backend.domain.user.entity.User;
import com.nongsabu.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final UserService userService;

    @Transactional
    public FarmResponse createFarm(Long userId, FarmRequest request) {
        User owner = userService.getUser(userId);
        Farm farm = farmRepository.save(Farm.builder()
                .owner(owner)
                .name(request.name())
                .location(request.location())
                .cultivationArea(request.cultivationArea())
                .cropSummary(request.cropSummary())
                .notes(request.notes())
                .build());
        return FarmResponse.from(farm);
    }

    @Transactional(readOnly = true)
    public List<FarmResponse> getFarms(Long userId) {
        return farmRepository.findAllByOwnerId(userId).stream().map(FarmResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FarmResponse getFarm(Long userId, Long farmId) {
        return FarmResponse.from(getOwnedFarm(userId, farmId));
    }

    @Transactional
    public FarmResponse updateFarm(Long userId, Long farmId, FarmRequest request) {
        Farm farm = getOwnedFarm(userId, farmId);
        farm.update(request.name(), request.location(), request.cultivationArea(), request.cropSummary(), request.notes());
        return FarmResponse.from(farm);
    }

    @Transactional(readOnly = true)
    public Farm getOwnedFarm(Long userId, Long farmId) {
        return farmRepository.findByIdAndOwnerId(farmId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "농장을 찾을 수 없습니다."));
    }
}

