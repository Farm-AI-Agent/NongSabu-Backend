package com.nongsabu.backend.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nongsabu.backend.domain.crop.entity.Crop;
import com.nongsabu.backend.domain.dashboard.dto.BriefingActionResponse;
import com.nongsabu.backend.domain.dashboard.dto.BriefingWarningResponse;
import com.nongsabu.backend.domain.dashboard.dto.FarmBriefingRequest;
import com.nongsabu.backend.domain.dashboard.dto.FarmBriefingResponse;
import com.nongsabu.backend.domain.dashboard.dto.WeatherSnapshotRequest;
import com.nongsabu.backend.domain.dashboard.entity.FarmBriefing;
import com.nongsabu.backend.domain.dashboard.repository.FarmBriefingRepository;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;
import com.nongsabu.backend.domain.farmprofile.repository.FarmProfileRepository;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.repository.MemberRepository;
import com.nongsabu.backend.domain.usercrop.entity.UserCrop;
import com.nongsabu.backend.domain.usercrop.repository.UserCropRepository;
import com.nongsabu.backend.infra.ai.llm.LlmClient;
import com.nongsabu.backend.support.TestFixtures;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FarmBriefingServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FarmProfileRepository farmProfileRepository;

    @Mock
    private UserCropRepository userCropRepository;

    @Mock
    private FarmBriefingRepository farmBriefingRepository;

    @Mock
    private LlmClient llmClient;

    private FarmBriefingService farmBriefingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        farmBriefingService = new FarmBriefingService(
                memberRepository,
                farmProfileRepository,
                userCropRepository,
                farmBriefingRepository,
                llmClient,
                objectMapper
        );
    }

    @Test
    void getTodayBriefingFallsBackToRuleActionsWhenLlmReturnsBlank() {
        Member member = TestFixtures.member(1L);
        FarmProfile profile = TestFixtures.farmProfile(1L, member);
        Crop crop = TestFixtures.crop(1L, "고추");
        UserCrop userCrop = TestFixtures.userCrop(1L, member, crop);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.of(profile));
        given(userCropRepository.findAllByMemberId(1L)).willReturn(List.of(userCrop));
        given(farmBriefingRepository.findFirstByMemberIdAndBriefingDateAndWeatherHashAndProfileHashOrderByCreatedAtDesc(
                any(), any(), any(), any()
        )).willReturn(Optional.empty());
        given(llmClient.generate(any(), any())).willReturn("");

        FarmBriefingResponse response = farmBriefingService.getTodayBriefing(1L, rainyRequest(false));

        assertThat(response.cached()).isFalse();
        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.source()).isEqualTo("RULE_FALLBACK");
        assertThat(response.cropNames()).contains("고추");
        assertThat(response.actions())
                .extracting(BriefingActionResponse::title)
                .anyMatch(title -> title.contains("배수로"));
        assertThat(response.warnings())
                .extracting(BriefingWarningResponse::type)
                .contains("RAIN");
        verify(farmBriefingRepository).save(any(FarmBriefing.class));
    }

    @Test
    void getTodayBriefingReturnsCachedPayloadWhenWeatherAndProfileMatch() throws Exception {
        Member member = TestFixtures.member(1L);
        FarmBriefingResponse cachedPayload = new FarmBriefingResponse(
                LocalDate.of(2026, 6, 18),
                Instant.parse("2026-06-17T22:00:00Z"),
                false,
                false,
                "RULE_FALLBACK",
                "next-morning-or-weather-change",
                "캐시된 브리핑입니다.",
                List.of(new BriefingActionResponse("LOW", "작업 기록 확인", "이전 브리핑을 재사용합니다.", "오전")),
                List.of(),
                List.of("토마토"),
                rainyRequest(false).weather()
        );
        FarmBriefing cached = FarmBriefing.builder()
                .member(member)
                .briefingDate(LocalDate.of(2026, 6, 18))
                .weatherHash("weather")
                .profileHash("profile")
                .weatherPayload("{}")
                .responsePayload(objectMapper.writeValueAsString(cachedPayload))
                .aiGenerated(false)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.empty());
        given(userCropRepository.findAllByMemberId(1L)).willReturn(List.of());
        given(farmBriefingRepository.findFirstByMemberIdAndBriefingDateAndWeatherHashAndProfileHashOrderByCreatedAtDesc(
                any(), any(), any(), any()
        )).willReturn(Optional.of(cached));

        FarmBriefingResponse response = farmBriefingService.getTodayBriefing(1L, rainyRequest(false));

        assertThat(response.cached()).isTrue();
        assertThat(response.summary()).isEqualTo("캐시된 브리핑입니다.");
        verify(llmClient, never()).generate(any(), any());
        verify(farmBriefingRepository, never()).save(any(FarmBriefing.class));
    }

    @Test
    void getTodayBriefingUsesStrictAiJsonWhenAvailable() {
        Member member = TestFixtures.member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(farmProfileRepository.findByMemberId(1L)).willReturn(Optional.empty());
        given(userCropRepository.findAllByMemberId(1L)).willReturn(List.of());
        given(farmBriefingRepository.findFirstByMemberIdAndBriefingDateAndWeatherHashAndProfileHashOrderByCreatedAtDesc(
                any(), any(), any(), any()
        )).willReturn(Optional.empty());
        given(llmClient.generate(any(), any())).willReturn("""
                {
                  "summary": "비가 오기 전 배수 점검이 우선입니다.",
                  "actions": [
                    {"priority": "HIGH", "title": "배수로 확인", "reason": "강수 확률이 높습니다.", "timeHint": "오전"}
                  ],
                  "warnings": [
                    {"type": "RAIN", "message": "방제는 비가 지난 뒤 검토하세요."}
                  ]
                }
                """);

        FarmBriefingResponse response = farmBriefingService.getTodayBriefing(1L, rainyRequest(false));

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.source()).isEqualTo("AI");
        assertThat(response.summary()).contains("배수");
        assertThat(response.actions()).hasSize(1);
    }

    private FarmBriefingRequest rainyRequest(boolean forceRefresh) {
        return new FarmBriefingRequest(
                new WeatherSnapshotRequest(
                        "전남 나주시",
                        OffsetDateTime.parse("2026-06-18T08:00:00+09:00"),
                        "흐림",
                        24.0,
                        20.0,
                        29.0,
                        85,
                        3.0,
                        80,
                        4.0,
                        "남서",
                        List.of("강수"),
                        null
                ),
                List.of(),
                forceRefresh
        );
    }
}
