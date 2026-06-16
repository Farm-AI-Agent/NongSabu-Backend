package com.nongsabu.backend.domain.farmprofile.service;

import static com.nongsabu.backend.support.TestFixtures.farmProfile;
import static com.nongsabu.backend.support.TestFixtures.crop;
import static com.nongsabu.backend.support.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.crop.repository.CropRepository;
import com.nongsabu.backend.domain.farmprofile.dto.FarmProfileDto;
import com.nongsabu.backend.domain.farmprofile.dto.FarmProfileRequest;
import com.nongsabu.backend.domain.farmprofile.entity.ExperienceLevel;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;
import com.nongsabu.backend.domain.farmprofile.repository.FarmProfileRepository;
import com.nongsabu.backend.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FarmProfileServiceTest {

    @Mock
    private FarmProfileRepository farmProfileRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CropRepository cropRepository;

    @InjectMocks
    private FarmProfileService farmProfileService;

    @Test
    void createFarmProfileWhenMemberHasNoProfile() {
        var member = member(1L);
        FarmProfileRequest request = new FarmProfileRequest("Naju", ExperienceLevel.BEGINNER, "small", 10L);
        FarmProfile saved = farmProfile(1L, member);

        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.empty());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(cropRepository.findById(10L)).willReturn(Optional.of(crop(10L, "포도")));
        given(farmProfileRepository.save(any(FarmProfile.class))).willReturn(saved);

        FarmProfileDto response = farmProfileService.create(1L, request);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.mainCropId()).isEqualTo(10L);
        assertThat(response.mainCropName()).isEqualTo("포도");
    }

    @Test
    void createFarmProfileFailsWhenProfileAlreadyExists() {
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.of(farmProfile(1L, member(1L))));

        assertThatThrownBy(() -> farmProfileService.create(
                1L,
                new FarmProfileRequest("Naju", ExperienceLevel.BEGINNER, "small", 10L)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateFarmProfileChangesExistingProfile() {
        FarmProfile profile = farmProfile(1L, member(1L));
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.of(profile));
        given(cropRepository.findById(11L)).willReturn(Optional.of(crop(11L, "토마토")));

        FarmProfileDto response = farmProfileService.update(
                1L,
                new FarmProfileRequest("Jeju", ExperienceLevel.ADVANCED, "large", 11L)
        );

        assertThat(response.region()).isEqualTo("Jeju");
        assertThat(response.experienceLevel()).isEqualTo(ExperienceLevel.ADVANCED);
        assertThat(response.mainCropName()).isEqualTo("토마토");
    }

    @Test
    void getMineFailsWhenProfileDoesNotExist() {
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> farmProfileService.getMine(1L))
                .isInstanceOf(BusinessException.class);
    }
}
