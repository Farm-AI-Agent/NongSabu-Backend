package com.nongsabu.backend.domain.policy.service;

import com.nongsabu.backend.domain.agri.dto.Gov24Response;
import com.nongsabu.backend.domain.agri.dto.YoungFarmerResponse;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;
import com.nongsabu.backend.domain.farmprofile.repository.FarmProfileRepository;
import com.nongsabu.backend.domain.policy.dto.PolicyRecommendationRequest;
import com.nongsabu.backend.domain.policy.dto.PolicyRecommendationResponse;
import com.nongsabu.backend.domain.policy.dto.PolicyPageResponse;
import com.nongsabu.backend.domain.policy.dto.PolicyEmbeddingStatusResponse;
import com.nongsabu.backend.domain.policy.dto.PolicySupportResponse;
import com.nongsabu.backend.domain.policy.dto.PolicySyncRequest;
import com.nongsabu.backend.domain.policy.dto.PolicySyncResponse;
import com.nongsabu.backend.domain.policy.entity.PolicySupport;
import com.nongsabu.backend.domain.policy.repository.PolicySupportRepository;
import com.nongsabu.backend.infra.external.Gov24Client;
import com.nongsabu.backend.infra.external.YoungFarmerClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySupportService {

    private static final String SOURCE_GOV24 = "GOV24";
    private static final String SOURCE_YOUNG_FARMER = "YOUNG_FARMER";
    private static final String CONTENT_TYPE = "policy_support";

    private final Gov24Client gov24Client;
    private final YoungFarmerClient youngFarmerClient;
    private final PolicySupportRepository policySupportRepository;
    private final FarmProfileRepository farmProfileRepository;
    private final VectorStore vectorStore;

    @Value("${app.policy.similarity-threshold:0.25}")
    private double similarityThreshold;

    @Transactional
    public PolicySyncResponse sync(PolicySyncRequest request) {
        List<PolicySupport> candidates = fetchCandidates(request);
        return saveAndEmbed(candidates);
    }

    @Transactional
    public PolicySyncResponse syncAll(PolicySyncRequest request) {
        int maxPages = request.safeMaxPages();
        int startPage = request.safePage();
        PolicySyncResponse total = new PolicySyncResponse(0, 0, 0);
        for (int offset = 0; offset < maxPages; offset++) {
            PolicySyncRequest pageRequest = new PolicySyncRequest(
                    request.includeGov24(),
                    request.includeYoungFarmer(),
                    request.keyword(),
                    startPage + offset,
                    request.safeSize(),
                    request.maxPages()
            );
            PolicySyncResponse pageResult = saveAndEmbed(fetchCandidates(pageRequest));
            total = new PolicySyncResponse(
                    total.fetchedCount() + pageResult.fetchedCount(),
                    total.savedCount() + pageResult.savedCount(),
                    total.embeddedCount() + pageResult.embeddedCount()
            );
            if (pageResult.fetchedCount() == 0) {
                break;
            }
        }
        return total;
    }

    private PolicySyncResponse saveAndEmbed(List<PolicySupport> candidates) {
        List<PolicySupport> toEmbed = new ArrayList<>();
        int savedCount = 0;

        for (PolicySupport candidate : candidates) {
            var existingPolicy = policySupportRepository.findBySourceAndExternalId(
                    candidate.getSource(),
                    candidate.getExternalId()
            );
            boolean changed = existingPolicy
                    .map(existing -> !existing.hasSameContentHash(candidate.getContentHash()))
                    .orElse(true);
            PolicySupport saved = existingPolicy
                    .map(existing -> {
                        if (changed) {
                            existing.updateFrom(candidate);
                        }
                        return existing;
                    })
                    .orElseGet(() -> policySupportRepository.save(candidate));

            if (changed) {
                savedCount++;
            }
            if (saved.getEmbeddingSyncedAt() == null) {
                toEmbed.add(saved);
            }
        }

        policySupportRepository.flush();
        int embeddedCount = embedPolicies(toEmbed);
        return new PolicySyncResponse(candidates.size(), savedCount, embeddedCount);
    }

    @Transactional(readOnly = true)
    public List<PolicySupportResponse> list(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return policySupportRepository.findAll(PageRequest.of(
                        0,
                        safeLimit,
                        Sort.by(Sort.Direction.DESC, "updatedAt")
                ))
                .map(PolicySupportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PolicyPageResponse search(
            int page,
            int size,
            String source,
            String region,
            String category,
            String keyword
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<PolicySupportResponse> result = policySupportRepository.search(
                        blankToNull(source),
                        blankToNull(region),
                        blankToNull(category),
                        blankToNull(keyword),
                        PageRequest.of(safePage, safeSize)
                )
                .map(PolicySupportResponse::from);
        return PolicyPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public PolicyRecommendationResponse recommend(Long memberId, PolicyRecommendationRequest request) {
        String query = buildRecommendationQuery(memberId, request);
        int topK = request.safeTopK();
        List<PolicySupportResponse> items = vectorRecommend(query, topK);
        if (items.isEmpty()) {
            items = list(topK);
        }
        return new PolicyRecommendationResponse(query, items);
    }

    @Transactional(readOnly = true)
    public PolicyEmbeddingStatusResponse embeddingStatus() {
        long policyCount = policySupportRepository.count();
        long embeddingSyncedCount = policySupportRepository.countByEmbeddingSyncedAtIsNotNull();
        long embeddingPendingCount = policySupportRepository.countByEmbeddingSyncedAtIsNull();
        int vectorSearchResultCount = 0;
        try {
            vectorSearchResultCount = vectorStore.similaritySearch(SearchRequest.builder()
                    .query("farmer subsidy support policy")
                    .topK(1)
                    .filterExpression("contentType == '" + CONTENT_TYPE + "'")
                    .build()).size();
        } catch (RuntimeException exception) {
            log.warn("Policy vector status check failed.", exception);
        }
        return new PolicyEmbeddingStatusResponse(
                policyCount,
                embeddingSyncedCount,
                embeddingPendingCount,
                vectorSearchResultCount > 0,
                vectorSearchResultCount
        );
    }

    private List<PolicySupport> fetchCandidates(PolicySyncRequest request) {
        List<PolicySupport> candidates = new ArrayList<>();
        int page = request.safePage();
        int size = request.safeSize();

        if (request.shouldIncludeGov24()) {
            Gov24Response response = gov24Client.searchServicesStructured(page, size);
            response.items().stream()
                    .map(this::fromGov24)
                    .forEach(candidates::add);
        }

        if (request.shouldIncludeYoungFarmer()) {
            YoungFarmerResponse response = youngFarmerClient.searchPoliciesStructured(request.keyword(), page, size);
            response.items().stream()
                    .map(this::fromYoungFarmer)
                    .forEach(candidates::add);
        }

        return candidates.stream()
                .filter(policy -> !isBlank(policy.getTitle()))
                .toList();
    }

    private int embedPolicies(List<PolicySupport> policies) {
        if (policies.isEmpty()) {
            return 0;
        }
        try {
            vectorStore.add(policies.stream()
                    .map(this::toVectorDocument)
                    .toList());
            policies.forEach(PolicySupport::markEmbeddingSynced);
            return policies.size();
        } catch (RuntimeException exception) {
            log.warn("Policy embedding sync failed. count={}", policies.size(), exception);
            return 0;
        }
    }

    private List<PolicySupportResponse> vectorRecommend(String query, int topK) {
        try {
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(Math.max(topK, 1) * 3)
                    .similarityThreshold(similarityThreshold)
                    .filterExpression("contentType == '" + CONTENT_TYPE + "'")
                    .build());

            Map<Long, Double> scoresByPolicyId = new LinkedHashMap<>();
            for (Document document : documents) {
                Long policyId = metadataLong(document.getMetadata(), "policyId");
                if (policyId != null) {
                    scoresByPolicyId.putIfAbsent(policyId, document.getScore());
                }
            }
            if (scoresByPolicyId.isEmpty()) {
                return List.of();
            }

            Map<Long, PolicySupport> policiesById = new LinkedHashMap<>();
            policySupportRepository.findAllByIdIn(scoresByPolicyId.keySet())
                    .forEach(policy -> policiesById.put(policy.getId(), policy));

            return scoresByPolicyId.entrySet().stream()
                    .map(entry -> {
                        PolicySupport policy = policiesById.get(entry.getKey());
                        return policy == null ? null : PolicySupportResponse.from(policy, entry.getValue());
                    })
                    .filter(item -> item != null)
                    .limit(topK)
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("Policy recommendation vector search failed. fallback will be used.", exception);
            return List.of();
        }
    }

    private String buildRecommendationQuery(Long memberId, PolicyRecommendationRequest request) {
        List<String> parts = new ArrayList<>();
        add(parts, "user request", request.query());
        add(parts, "region", request.region());
        add(parts, "crop", request.cropName());
        add(parts, "experience", request.experienceLevel() == null ? null : request.experienceLevel().name());
        add(parts, "farm size", request.farmSize());
        add(parts, "age", request.age());
        add(parts, "young farmer eligible", request.youngFarmerEligible());
        add(parts, "farming start year", request.farmingStartYear());
        add(parts, "farming type", request.farmingType());
        add(parts, "residence region", request.residenceRegion());
        add(parts, "farmland region", request.farmlandRegion());
        add(parts, "primary crop", request.primaryCropName());
        add(parts, "secondary crops", request.secondaryCropNames());
        add(parts, "cultivation area", request.cultivationArea());
        add(parts, "cultivation type", request.cultivationType());
        add(parts, "applicant type", request.applicantType());
        add(parts, "registered farm business", request.registeredFarmBusiness());
        add(parts, "annual sales range", request.annualSalesRange());
        add(parts, "desired support types", request.desiredSupportTypes());
        add(parts, "self contribution available", request.selfContributionAvailable());
        add(parts, "already received policies", request.receivedPolicyNames());
        add(parts, "application period preference", request.applicationPeriodPreference());

        if (memberId != null && request.shouldIncludeProfile()) {
            farmProfileRepository.findByMemberId(memberId).ifPresent(profile -> addProfile(parts, profile));
        }

        if (parts.isEmpty()) {
            return "beginner farmer policy subsidy support project";
        }
        return String.join("\n", parts);
    }

    private void addProfile(List<String> parts, FarmProfile profile) {
        add(parts, "profile region", profile.getRegion());
        add(parts, "profile experience", profile.getExperienceLevel() == null ? null : profile.getExperienceLevel().name());
        add(parts, "profile farm size", profile.getFarmSize());
        if (profile.getMainCrop() != null) {
            add(parts, "profile crop", profile.getMainCrop().getName());
            add(parts, "profile crop category", profile.getMainCrop().getCategory());
        }
        add(parts, "profile age", profile.getAge());
        add(parts, "profile young farmer eligible", profile.getYoungFarmerEligible());
        add(parts, "profile farming start year", profile.getFarmingStartYear());
        add(parts, "profile farming type", profile.getFarmingType());
        add(parts, "profile residence region", profile.getResidenceRegion());
        add(parts, "profile farmland region", profile.getFarmlandRegion());
        add(parts, "profile primary crop", profile.getPrimaryCropName());
        add(parts, "profile secondary crops", profile.getSecondaryCropNames());
        add(parts, "profile cultivation area", profile.getCultivationArea());
        add(parts, "profile cultivation type", profile.getCultivationType());
        add(parts, "profile applicant type", profile.getApplicantType());
        add(parts, "profile registered farm business", profile.getRegisteredFarmBusiness());
        add(parts, "profile annual sales range", profile.getAnnualSalesRange());
        add(parts, "profile desired support types", profile.getDesiredSupportTypes());
        add(parts, "profile self contribution available", profile.getSelfContributionAvailable());
        add(parts, "profile already received policies", profile.getReceivedPolicyNames());
        add(parts, "profile application period preference", profile.getApplicationPeriodPreference());
    }

    private Document toVectorDocument(PolicySupport policy) {
        return Document.builder()
                .id(vectorDocumentId(policy.getId()))
                .text(policy.getContentText())
                .metadata(Map.of(
                        "contentType", CONTENT_TYPE,
                        "policyId", String.valueOf(policy.getId()),
                        "source", policy.getSource(),
                        "externalId", policy.getExternalId(),
                        "title", policy.getTitle()
                ))
                .build();
    }

    private PolicySupport fromGov24(Gov24Response.Item item) {
        String externalId = firstNonBlank(item.serviceId(), stableExternalId(SOURCE_GOV24, item.serviceName()));
        String content = buildContent(Map.of(
                "source", SOURCE_GOV24,
                "title", value(item.serviceName()),
                "summary", value(item.servicePurpose()),
                "target", value(item.targetGroup()),
                "category", value(item.serviceField()),
                "period", value(item.applyDeadline()),
                "method", value(item.applyMethod()),
                "department", value(item.department()),
                "url", value(item.detailUrl())
        ));
        return PolicySupport.builder()
                .source(SOURCE_GOV24)
                .externalId(externalId)
                .title(firstNonBlank(item.serviceName(), "GOV24 policy"))
                .summary(trimToLength(item.servicePurpose(), 2000))
                .targetGroup(trimToLength(item.targetGroup(), 1000))
                .category(trimToLength(item.serviceField(), 120))
                .applicationPeriod(trimToLength(item.applyDeadline(), 120))
                .applyMethod(trimToLength(item.applyMethod(), 1000))
                .department(trimToLength(item.department(), 200))
                .detailUrl(trimToLength(item.detailUrl(), 1000))
                .contentText(content)
                .contentHash(sha256(content))
                .build();
    }

    private PolicySupport fromYoungFarmer(YoungFarmerResponse.Item item) {
        String period = joinPeriod(item.applStDt(), item.applEdDt());
        String content = buildContent(Map.of(
                "source", SOURCE_YOUNG_FARMER,
                "title", value(item.title()),
                "summary", value(item.summary()),
                "region", value(item.area1Nm()),
                "period", value(period),
                "department", value(item.chargeAgency()),
                "contact", value(item.chargeTel()),
                "url", value(item.infoUrl())
        ));
        return PolicySupport.builder()
                .source(SOURCE_YOUNG_FARMER)
                .externalId(firstNonBlank(item.seq(), stableExternalId(SOURCE_YOUNG_FARMER, item.title())))
                .title(firstNonBlank(item.title(), "Young farmer policy"))
                .summary(trimToLength(item.summary(), 2000))
                .region(trimToLength(item.area1Nm(), 100))
                .applicationPeriod(trimToLength(period, 120))
                .department(trimToLength(item.chargeAgency(), 200))
                .contact(trimToLength(item.chargeTel(), 100))
                .detailUrl(trimToLength(item.infoUrl(), 1000))
                .contentText(content)
                .contentHash(sha256(content))
                .build();
    }

    private String buildContent(Map<String, String> fields) {
        return fields.entrySet().stream()
                .filter(entry -> !isBlank(entry.getValue()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private void add(List<String> parts, String label, String value) {
        if (!isBlank(value)) {
            parts.add(label + ": " + value.trim());
        }
    }

    private void add(List<String> parts, String label, Integer value) {
        if (value != null) {
            parts.add(label + ": " + value);
        }
    }

    private void add(List<String> parts, String label, Boolean value) {
        if (value != null) {
            parts.add(label + ": " + value);
        }
    }

    private Long metadataLong(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String vectorDocumentId(Long policyId) {
        return UUID.nameUUIDFromBytes(("policy-support:" + policyId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String stableExternalId(String source, String title) {
        return sha256(source + ":" + value(title)).substring(0, 32);
    }

    private String joinPeriod(String start, String end) {
        if (isBlank(start)) {
            return value(end);
        }
        if (isBlank(end)) {
            return start.trim();
        }
        return start.trim() + " ~ " + end.trim();
    }

    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first.trim();
    }

    private String trimToLength(String value, int maxLength) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String value(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }
}
