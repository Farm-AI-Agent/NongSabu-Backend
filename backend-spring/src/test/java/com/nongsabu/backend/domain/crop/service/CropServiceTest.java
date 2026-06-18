package com.nongsabu.backend.domain.crop.service;

import static com.nongsabu.backend.support.TestFixtures.crop;
import static com.nongsabu.backend.support.TestFixtures.farm;
import static com.nongsabu.backend.support.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.crop.dto.CropResponse;
import com.nongsabu.backend.domain.crop.dto.FarmCropRequest;
import com.nongsabu.backend.domain.crop.dto.FarmCropResponse;
import com.nongsabu.backend.domain.crop.entity.FarmCrop;
import com.nongsabu.backend.domain.crop.repository.CropRepository;
import com.nongsabu.backend.domain.crop.repository.FarmCropRepository;
import com.nongsabu.backend.domain.farm.service.FarmService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CropServiceTest {

    @Mock
    private CropRepository cropRepository;

    @Mock
    private FarmCropRepository farmCropRepository;

    @Mock
    private FarmService farmService;

    @InjectMocks
    private CropService cropService;

    @Test
    void getAllCropsReturnsSeedCandidates() {
        given(cropRepository.findAll()).willReturn(List.of(
                crop(1L, "grape"),
                crop(2L, "tomato")
        ));

        List<CropResponse> response = cropService.getAllCrops();

        assertThat(response).extracting(CropResponse::name)
                .containsExactly("grape", "tomato");
    }

    @Test
    void getCropReturnsSingleCrop() {
        given(cropRepository.findById(1L)).willReturn(Optional.of(crop(1L, "grape")));

        CropResponse response = cropService.getCrop(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("grape");
    }

    @Test
    void getCropFailsWhenMissing() {
        given(cropRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cropService.getCrop(99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registerFarmCropCreatesFarmCropWhenNotDuplicated() {
        var member = member(1L);
        var farm = farm(10L, member);
        var crop = crop(20L, "grape");
        given(farmService.getOwnedFarm(1L, 10L)).willReturn(farm);
        given(cropRepository.findById(20L)).willReturn(Optional.of(crop));
        given(farmCropRepository.existsByFarmIdAndCropId(10L, 20L)).willReturn(false);
        given(farmCropRepository.save(any(FarmCrop.class))).willReturn(FarmCrop.builder()
                .id(30L)
                .farm(farm)
                .crop(crop)
                .status("ACTIVE")
                .build());

        FarmCropResponse response = cropService.registerFarmCrop(1L, 10L, new FarmCropRequest(20L, "ACTIVE"));

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.cropName()).isEqualTo("grape");
    }

    @Test
    void registerFarmCropFailsWhenAlreadyRegistered() {
        given(farmService.getOwnedFarm(1L, 10L)).willReturn(farm(10L, member(1L)));
        given(cropRepository.findById(20L)).willReturn(Optional.of(crop(20L, "grape")));
        given(farmCropRepository.existsByFarmIdAndCropId(10L, 20L)).willReturn(true);

        assertThatThrownBy(() -> cropService.registerFarmCrop(1L, 10L, new FarmCropRequest(20L, "ACTIVE")))
                .isInstanceOf(BusinessException.class);
    }
}
