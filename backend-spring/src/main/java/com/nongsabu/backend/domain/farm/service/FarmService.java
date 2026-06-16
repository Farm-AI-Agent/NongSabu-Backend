package com.nongsabu.backend.domain.farm.service;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.farm.dto.FarmRequest;
import com.nongsabu.backend.domain.farm.dto.FarmResponse;
import com.nongsabu.backend.domain.farm.entity.Farm;
import com.nongsabu.backend.domain.farm.repository.FarmRepository;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.service.MemberService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final MemberService memberService;

    @Transactional
    public FarmResponse createFarm(Long memberId, FarmRequest request) {
        Member member = memberService.getMember(memberId);
        Farm farm = farmRepository.save(Farm.builder()
                .member(member)
                .name(request.name())
                .location(request.location())
                .cultivationArea(request.cultivationArea())
                .notes(request.notes())
                .build());
        return FarmResponse.from(farm);
    }

    @Transactional(readOnly = true)
    public List<FarmResponse> getFarms(Long memberId) {
        return farmRepository.findAllByMemberId(memberId).stream().map(FarmResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FarmResponse getFarm(Long memberId, Long farmId) {
        return FarmResponse.from(getOwnedFarm(memberId, farmId));
    }

    @Transactional
    public FarmResponse updateFarm(Long memberId, Long farmId, FarmRequest request) {
        Farm farm = getOwnedFarm(memberId, farmId);
        farm.update(request.name(), request.location(), request.cultivationArea(), request.notes());
        return FarmResponse.from(farm);
    }

    @Transactional(readOnly = true)
    public Farm getOwnedFarm(Long memberId, Long farmId) {
        return farmRepository.findByIdAndMemberId(farmId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "농장을 찾을 수 없습니다."));
    }
}
