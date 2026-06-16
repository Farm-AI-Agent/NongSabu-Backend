package com.nongsabu.backend.domain.farm.service;

import static com.nongsabu.backend.support.TestFixtures.farm;
import static com.nongsabu.backend.support.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.farm.dto.FarmRequest;
import com.nongsabu.backend.domain.farm.dto.FarmResponse;
import com.nongsabu.backend.domain.farm.entity.Farm;
import com.nongsabu.backend.domain.farm.repository.FarmRepository;
import com.nongsabu.backend.domain.member.service.MemberService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private FarmService farmService;

    @Test
    void createFarmUsesCurrentMember() {
        var member = member(1L);
        given(memberService.getMember(1L)).willReturn(member);
        given(farmRepository.save(any(Farm.class))).willReturn(farm(10L, member));

        FarmResponse response = farmService.createFarm(
                1L,
                new FarmRequest("farm-10", "Naju", "greenhouse", "notes")
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("farm-10");
    }

    @Test
    void getFarmsReturnsOnlyMemberFarmsFromRepositoryQuery() {
        given(farmRepository.findAllByMemberId(1L)).willReturn(List.of(farm(10L, member(1L))));

        List<FarmResponse> response = farmService.getFarms(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(10L);
    }

    @Test
    void updateFarmChangesOwnedFarm() {
        Farm farm = farm(10L, member(1L));
        given(farmRepository.findByIdAndMemberId(10L, 1L)).willReturn(Optional.of(farm));

        FarmResponse response = farmService.updateFarm(
                1L,
                10L,
                new FarmRequest("updated", "Jeju", "field", "updated-notes")
        );

        assertThat(response.name()).isEqualTo("updated");
        assertThat(response.location()).isEqualTo("Jeju");
        assertThat(farm.getNotes()).isEqualTo("updated-notes");
    }

    @Test
    void getOwnedFarmFailsWhenFarmIsMissingOrNotOwned() {
        given(farmRepository.findByIdAndMemberId(10L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> farmService.getOwnedFarm(2L, 10L))
                .isInstanceOf(BusinessException.class);
    }
}
