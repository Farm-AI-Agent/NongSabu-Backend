package com.nongsabu.backend.domain.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.common.exception.ErrorCode;
import com.nongsabu.backend.domain.dashboard.dto.BriefingActionResponse;
import com.nongsabu.backend.domain.dashboard.dto.BriefingWarningResponse;
import com.nongsabu.backend.domain.dashboard.dto.FarmBriefingRequest;
import com.nongsabu.backend.domain.dashboard.dto.FarmBriefingResponse;
import com.nongsabu.backend.domain.dashboard.dto.WeatherForecastRequest;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmBriefingService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int MAX_ACTIONS = 3;

    private final MemberRepository memberRepository;
    private final FarmProfileRepository farmProfileRepository;
    private final UserCropRepository userCropRepository;
    private final FarmBriefingRepository farmBriefingRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public FarmBriefingResponse getTodayBriefing(Long memberId, FarmBriefingRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Optional<FarmProfile> profile = farmProfileRepository.findByMemberId(memberId);
        List<UserCrop> userCrops = userCropRepository.findAllByMemberId(memberId);

        LocalDate briefingDate = resolveBriefingDate(request.weather());
        List<String> cropNames = cropNames(profile, userCrops);
        String weatherPayload = toJson(request);
        String weatherHash = sha256(weatherPayload);
        String profileHash = sha256(profileContext(profile, userCrops, cropNames));

        if (!request.shouldForceRefresh()) {
            Optional<FarmBriefing> cached = farmBriefingRepository
                    .findFirstByMemberIdAndBriefingDateAndWeatherHashAndProfileHashOrderByCreatedAtDesc(
                            memberId,
                            briefingDate,
                            weatherHash,
                            profileHash
                    );
            if (cached.isPresent()) {
                return parseResponse(cached.get().getResponsePayload()).withCacheState(true);
            }
        }

        FarmBriefingResponse response = generateBriefing(briefingDate, request, profile, userCrops, cropNames);
        farmBriefingRepository.save(FarmBriefing.builder()
                .member(member)
                .briefingDate(briefingDate)
                .weatherHash(weatherHash)
                .profileHash(profileHash)
                .weatherPayload(weatherPayload)
                .responsePayload(toJson(response))
                .aiGenerated(response.aiGenerated())
                .build());
        return response;
    }

    private FarmBriefingResponse generateBriefing(
            LocalDate briefingDate,
            FarmBriefingRequest request,
            Optional<FarmProfile> profile,
            List<UserCrop> userCrops,
            List<String> cropNames
    ) {
        AiBriefingPayload rulePayload = ruleBasedPayload(request.weather(), request.forecast(), profile, cropNames);
        AiBriefingPayload aiPayload = generateAiPayload(request, profile, userCrops, cropNames, rulePayload);
        boolean aiGenerated = aiPayload != null;
        AiBriefingPayload payload = aiGenerated ? aiPayload : rulePayload;

        return new FarmBriefingResponse(
                briefingDate,
                Instant.now(),
                false,
                aiGenerated,
                aiGenerated ? "AI" : "RULE_FALLBACK",
                "next-morning-or-weather-change",
                payload.summary(),
                limitActions(payload.actions()),
                nullToEmpty(payload.warnings()),
                cropNames,
                request.weather()
        );
    }

    private AiBriefingPayload generateAiPayload(
            FarmBriefingRequest request,
            Optional<FarmProfile> profile,
            List<UserCrop> userCrops,
            List<String> cropNames,
            AiBriefingPayload rulePayload
    ) {
        try {
            String answer = llmClient.generate(aiPrompt(request, profile, userCrops, cropNames, rulePayload), "");
            if (answer == null || answer.isBlank()) {
                return null;
            }
            return objectMapper.readValue(extractJson(answer), AiBriefingPayload.class);
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Farm briefing AI generation failed. Falling back to rule payload.", exception);
            return null;
        }
    }

    private AiBriefingPayload ruleBasedPayload(
            WeatherSnapshotRequest weather,
            List<WeatherForecastRequest> forecast,
            Optional<FarmProfile> profile,
            List<String> cropNames
    ) {
        List<BriefingActionResponse> actions = new ArrayList<>();
        List<BriefingWarningResponse> warnings = new ArrayList<>();
        String cultivationType = profile.map(FarmProfile::getCultivationType).orElse("");
        boolean greenhouse = containsAny(cultivationType, "시설", "greenhouse", "house");

        if (number(weather.precipitationMm()) >= 1.0 || number(weather.precipitationProbability()) >= 60) {
            actions.add(new BriefingActionResponse("HIGH", "배수로와 물 고임 확인", "비 예보가 있어 뿌리 과습과 토양 유실 위험을 먼저 줄여야 합니다.", "비 오기 전"));
            warnings.add(new BriefingWarningResponse("RAIN", "강수 가능성이 높으니 관수와 방제 작업은 날씨가 지난 뒤로 미루는 편이 좋습니다."));
        }
        if (number(weather.humidityPercent()) >= 80) {
            actions.add(new BriefingActionResponse("HIGH", "잎과 줄기 병해 징후 점검", "습도가 높으면 곰팡이성 병해가 빠르게 번질 수 있습니다.", "오전 또는 비가 그친 뒤"));
            warnings.add(new BriefingWarningResponse("HUMIDITY", "습도가 높아 병해 위험이 올라갈 수 있습니다."));
        }
        if (number(weather.maxTemperatureC()) >= 33 || number(weather.temperatureC()) >= 32) {
            actions.add(new BriefingActionResponse("HIGH", greenhouse ? "시설 환기와 차광 확인" : "고온 시간대 작업 피하기", "고온 조건에서는 작물 스트레스와 작업자 온열 위험이 커집니다.", "한낮 전후"));
            warnings.add(new BriefingWarningResponse("HEAT", "고온 피해 가능성이 있으니 관수 상태와 차광 여부를 확인하세요."));
        }
        if (number(weather.minTemperatureC()) <= 5 && hasValue(weather.minTemperatureC())) {
            actions.add(new BriefingActionResponse("HIGH", "저온 피해 대비", "최저기온이 낮아 어린 작물과 시설 내부 온도 관리가 필요합니다.", "해 지기 전"));
            warnings.add(new BriefingWarningResponse("COLD", "저온으로 생육 지연이나 냉해 가능성이 있습니다."));
        }
        if (number(weather.windSpeedMs()) >= 8) {
            actions.add(new BriefingActionResponse("MEDIUM", greenhouse ? "하우스 고정 상태 확인" : "지주대와 유인끈 확인", "강한 바람은 시설물과 줄기 손상을 만들 수 있습니다.", "바람 강해지기 전"));
            warnings.add(new BriefingWarningResponse("WIND", "바람이 강하면 시설물과 지주 고정 상태를 먼저 확인하세요."));
        }
        if (actions.isEmpty()) {
            actions.add(new BriefingActionResponse("MEDIUM", "토양 수분과 생육 상태 확인", "큰 기상 위험은 적지만 작물 상태 확인이 오늘 작업의 기준이 됩니다.", "오전"));
            actions.add(new BriefingActionResponse("LOW", "작업 기록 남기기", "관수, 병해 징후, 생육 변화를 기록하면 다음 추천이 더 정확해집니다.", "작업 후"));
        }

        String cropLabel = cropNames.isEmpty() ? "등록된 작물" : String.join(", ", cropNames);
        String summary = "%s 기준으로 오늘은 %s을 우선 확인하세요.".formatted(
                weather.location() == null || weather.location().isBlank() ? "현재 날씨" : weather.location() + " 날씨",
                actions.get(0).title()
        );
        if (!cropNames.isEmpty()) {
            summary = "%s 재배 상태와 날씨를 함께 보면, 오늘은 %s을 우선 확인하는 날입니다.".formatted(cropLabel, actions.get(0).title());
        }

        if (forecast != null && forecast.stream().anyMatch(day -> number(day.precipitationProbability()) >= 60)) {
            actions.add(new BriefingActionResponse("MEDIUM", "비 오기 전 작업 순서 조정", "예보에 비 가능성이 있어 방제나 노지 작업은 맑은 시간대로 당기는 것이 좋습니다.", "이번 주"));
        }

        return new AiBriefingPayload(summary, limitActions(actions), warnings);
    }

    private String aiPrompt(
            FarmBriefingRequest request,
            Optional<FarmProfile> profile,
            List<UserCrop> userCrops,
            List<String> cropNames,
            AiBriefingPayload rulePayload
    ) {
        return """
                아래 데이터로 메인 화면에 보여줄 오늘의 농사 브리핑을 작성하세요.
                반드시 JSON만 반환하고, 설명문이나 마크다운 코드는 쓰지 마세요.

                JSON 스키마:
                {
                  "summary": "한 문장 요약",
                  "actions": [
                    {"priority": "HIGH|MEDIUM|LOW", "title": "짧은 작업명", "reason": "근거 한 문장", "timeHint": "권장 시간"}
                  ],
                  "warnings": [
                    {"type": "RAIN|HUMIDITY|HEAT|COLD|WIND|NORMAL", "message": "주의 문장"}
                  ]
                }

                작성 규칙:
                - 오늘 할 일은 최대 3개만 반환합니다.
                - 초보 농가도 바로 이해할 수 있게 씁니다.
                - 농약, 방제 약제, 질병 진단은 단정하지 않습니다.
                - 날씨 데이터와 작물/농장 정보에 근거한 행동만 제안합니다.
                - 사용자가 이미 아는 작물명을 크게 설명하지 말고, 행동 중심으로 씁니다.

                사용자 작물:
                %s

                농장 프로필:
                %s

                등록 작물 상세:
                %s

                오늘 날씨:
                %s

                예보:
                %s

                룰 기반 후보:
                %s
                """.formatted(
                cropNames.isEmpty() ? "없음" : String.join(", ", cropNames),
                profile.map(this::profileText).orElse("없음"),
                userCropText(userCrops),
                toJson(request.weather()),
                toJson(request.forecast()),
                toJson(rulePayload)
        );
    }

    private List<String> cropNames(Optional<FarmProfile> profile, List<UserCrop> userCrops) {
        Set<String> names = new LinkedHashSet<>();
        profile.map(FarmProfile::getMainCrop)
                .ifPresent(crop -> addIfPresent(names, crop.getName()));
        profile.map(FarmProfile::getPrimaryCropName)
                .ifPresent(name -> addIfPresent(names, name));
        profile.map(FarmProfile::getSecondaryCropNames)
                .ifPresent(value -> {
                    for (String name : value.split(",")) {
                        addIfPresent(names, name);
                    }
                });
        for (UserCrop userCrop : userCrops) {
            if (userCrop.getCrop() != null) {
                addIfPresent(names, userCrop.getCrop().getName());
            }
        }
        return List.copyOf(names);
    }

    private String profileContext(Optional<FarmProfile> profile, List<UserCrop> userCrops, List<String> cropNames) {
        return profile.map(this::profileText).orElse("no-profile") + "\n" + userCropText(userCrops) + "\n" + cropNames;
    }

    private String profileText(FarmProfile profile) {
        return """
                region=%s
                farmlandRegion=%s
                experienceLevel=%s
                cultivationType=%s
                cultivationArea=%s
                farmingType=%s
                registeredFarmBusiness=%s
                """.formatted(
                profile.getRegion(),
                profile.getFarmlandRegion(),
                profile.getExperienceLevel(),
                profile.getCultivationType(),
                profile.getCultivationArea(),
                profile.getFarmingType(),
                profile.getRegisteredFarmBusiness()
        );
    }

    private String userCropText(List<UserCrop> userCrops) {
        if (userCrops == null || userCrops.isEmpty()) {
            return "없음";
        }
        return userCrops.stream()
                .map(userCrop -> "%s(area=%s,memo=%s)".formatted(
                        userCrop.getCrop() == null ? "unknown" : userCrop.getCrop().getName(),
                        userCrop.getCultivationArea(),
                        userCrop.getMemo()
                ))
                .toList()
                .toString();
    }

    private LocalDate resolveBriefingDate(WeatherSnapshotRequest weather) {
        if (weather.observedAt() != null) {
            return weather.observedAt().atZoneSameInstant(KOREA_ZONE).toLocalDate();
        }
        return LocalDate.now(KOREA_ZONE);
    }

    private FarmBriefingResponse parseResponse(String responsePayload) {
        try {
            return objectMapper.readValue(responsePayload, FarmBriefingResponse.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "저장된 농사 브리핑을 읽을 수 없습니다.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "농사 브리핑 데이터를 직렬화할 수 없습니다.");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "SHA-256 해시를 계산할 수 없습니다.");
        }
    }

    private String extractJson(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstBrace = trimmed.indexOf('{');
            int lastBrace = trimmed.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        return trimmed;
    }

    private List<BriefingActionResponse> limitActions(List<BriefingActionResponse> actions) {
        return nullToEmpty(actions).stream()
                .limit(MAX_ACTIONS)
                .toList();
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private boolean containsAny(String value, String... needles) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValue(Double value) {
        return value != null;
    }

    private double number(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private record AiBriefingPayload(
            String summary,
            List<BriefingActionResponse> actions,
            List<BriefingWarningResponse> warnings
    ) {
    }
}
