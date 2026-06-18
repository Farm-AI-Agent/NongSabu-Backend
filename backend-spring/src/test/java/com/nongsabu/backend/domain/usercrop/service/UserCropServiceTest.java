package com.nongsabu.backend.domain.usercrop.service;

import static com.nongsabu.backend.support.TestFixtures.crop;
import static com.nongsabu.backend.support.TestFixtures.member;
import static com.nongsabu.backend.support.TestFixtures.userCrop;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.crop.repository.CropRepository;
import com.nongsabu.backend.domain.member.repository.MemberRepository;
import com.nongsabu.backend.domain.usercrop.dto.UserCropDto;
import com.nongsabu.backend.domain.usercrop.dto.UserCropRequest;
import com.nongsabu.backend.domain.usercrop.entity.UserCrop;
import com.nongsabu.backend.domain.usercrop.repository.UserCropRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCropServiceTest {

    @Mock
    private UserCropRepository userCropRepository;

    @Mock
    private CropRepository cropRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private UserCropService userCropService;

    @Test
    void createUserCropWithOwnedMemberAndExistingCrop() {
        var member = member(1L);
        var crop = crop(10L, "grape");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(cropRepository.findById(10L)).willReturn(Optional.of(crop));
        given(userCropRepository.save(any(UserCrop.class))).willReturn(userCrop(100L, member, crop));

        UserCropDto response = userCropService.create(1L, new UserCropRequest(10L, "greenhouse-1", "first crop"));

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.cropId()).isEqualTo(10L);
        assertThat(response.cropName()).isEqualTo("grape");
    }

    @Test
    void createUserCropFailsWhenCropDoesNotExist() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L)));
        given(cropRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userCropService.create(1L, new UserCropRequest(99L, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getMineReturnsOnlyMemberCropsFromRepositoryQuery() {
        var member = member(1L);
        given(userCropRepository.findAllByMemberId(1L))
                .willReturn(List.of(userCrop(1L, member, crop(10L, "grape"))));

        List<UserCropDto> response = userCropService.getMine(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).memberId()).isEqualTo(1L);
    }

    @Test
    void deleteRemovesOnlyOwnedUserCrop() {
        var userCrop = userCrop(1L, member(1L), crop(10L, "grape"));
        given(userCropRepository.findByIdAndMemberId(1L, 1L)).willReturn(Optional.of(userCrop));

        userCropService.delete(1L, 1L);

        verify(userCropRepository).delete(userCrop);
    }

    @Test
    void deleteFailsWhenUserCropIsNotOwnedByMember() {
        given(userCropRepository.findByIdAndMemberId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userCropService.delete(2L, 1L))
                .isInstanceOf(BusinessException.class);
    }
}
