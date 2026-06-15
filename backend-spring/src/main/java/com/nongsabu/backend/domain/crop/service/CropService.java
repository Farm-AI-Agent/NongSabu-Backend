package com.nongsabu.backend.domain.crop.service;

import java.util.List;
import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.crop.dto.CropResponse;
import com.nongsabu.backend.domain.crop.dto.FarmCropRequest;
import com.nongsabu.backend.domain.crop.dto.FarmCropResponse;
import com.nongsabu.backend.domain.crop.entity.Crop;
import com.nongsabu.backend.domain.crop.entity.FarmCrop;
import com.nongsabu.backend.domain.crop.repository.CropRepository;
import com.nongsabu.backend.domain.crop.repository.FarmCropRepository;
import com.nongsabu.backend.domain.farm.entity.Farm;
import com.nongsabu.backend.domain.farm.service.FarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CropService {

    private final CropRepository cropRepository;
    private final FarmCropRepository farmCropRepository;
    private final FarmService farmService;

    @Transactional(readOnly = true)
    public List<CropResponse> getAllCrops() {
        return cropRepository.findAll().stream().map(CropResponse::from).toList();
    }

    @Transactional
    public FarmCropResponse registerFarmCrop(Long userId, Long farmId, FarmCropRequest request) {
        Farm farm = farmService.getOwnedFarm(userId, farmId);
        Crop crop = cropRepository.findById(request.cropId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "작물을 찾을 수 없습니다."));

        if (farmCropRepository.existsByFarmIdAndCropId(farmId, request.cropId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 등록된 작물입니다.");
        }

        FarmCrop farmCrop = farmCropRepository.save(FarmCrop.builder()
                .farm(farm)
                .crop(crop)
                .status(request.status())
                .build());
        return FarmCropResponse.from(farmCrop);
    }
}

