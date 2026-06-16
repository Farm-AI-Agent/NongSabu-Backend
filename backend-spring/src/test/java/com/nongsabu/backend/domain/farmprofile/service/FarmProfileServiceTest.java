package com.nongsabu.backend.domain.farmprofile.service;

import static com.nongsabu.backend.support.TestFixtures.farmProfile;
import static com.nongsabu.backend.support.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nongsabu.backend.common.exception.BusinessException;
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

    @InjectMocks
    private FarmProfileService farmProfileService;

    @Test
    void createFarmProfileWhenMemberHasNoProfile() {
        var member = member(1L);
        FarmProfileRequest request = new FarmProfileRequest("Naju", ExperienceLevel.BEGINNER, "small", "grape");
        FarmProfile saved = farmProfile(1L, member);

        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.empty());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(farmProfileRepository.save(any(FarmProfile.class))).willReturn(saved);

        FarmProfileDto response = farmProfileService.create(1L, request);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.mainCrop()).isEqualTo("grape");
    }

    @Test
    void createFarmProfileFailsWhenProfileAlreadyExists() {
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.of(farmProfile(1L, member(1L))));

        assertThatThrownBy(() -> farmProfileService.create(
                1L,
                new FarmProfileRequest("Naju", ExperienceLevel.BEGINNER, "small", "grape")
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateFarmProfileChangesExistingProfile() {
        FarmProfile profile = farmProfile(1L, member(1L));
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.of(profile));

        FarmProfileDto response = farmProfileService.update(
                1L,
                new FarmProfileRequest("Jeju", ExperienceLevel.ADVANCED, "large", "tomato")
        );

        assertThat(response.region()).isEqualTo("Jeju");
        assertThat(response.experienceLevel()).isEqualTo(ExperienceLevel.ADVANCED);
        assertThat(response.mainCrop()).isEqualTo("tomato");
    }

    @Test
    void getMineFailsWhenProfileDoesNotExist() {
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> farmProfileService.getMine(1L))
                .isInstanceOf(BusinessException.class);
    }
}
