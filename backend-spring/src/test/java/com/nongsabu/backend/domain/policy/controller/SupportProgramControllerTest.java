package com.nongsabu.backend.domain.policy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nongsabu.backend.domain.policy.dto.PolicyPageResponse;
import com.nongsabu.backend.domain.policy.dto.PolicyRecommendationResponse;
import com.nongsabu.backend.domain.policy.dto.PolicySupportResponse;
import com.nongsabu.backend.domain.policy.service.PolicySupportService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SupportProgramControllerTest {

    @Mock
    private PolicySupportService policySupportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SupportProgramController(policySupportService)).build();
    }

    @Test
    void listUsesSupportProgramsCompatibilityPath() throws Exception {
        given(policySupportService.search(0, 10, null, null, null, "청년"))
                .willReturn(new PolicyPageResponse(
                        0,
                        10,
                        1,
                        1,
                        List.of(policyResponse())
                ));

        mockMvc.perform(get("/api/v1/support-programs")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "청년"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("청년농업인 영농정착지원사업"));

        verify(policySupportService).search(0, 10, null, null, null, "청년");
    }

    @Test
    void recommendedUsesSupportProgramsCompatibilityPath() throws Exception {
        given(policySupportService.recommend(eq(null), any()))
                .willReturn(new PolicyRecommendationResponse(
                        "청년농",
                        List.of(policyResponse())
                ));

        mockMvc.perform(get("/api/v1/support-programs/recommended")
                        .param("query", "청년농")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("청년농업인 영농정착지원사업"));
    }

    @Test
    void detailUsesSupportProgramsCompatibilityPath() throws Exception {
        given(policySupportService.get(1L)).willReturn(policyResponse());

        mockMvc.perform(get("/api/v1/support-programs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("청년농업인 영농정착지원사업"));
    }

    private PolicySupportResponse policyResponse() {
        return new PolicySupportResponse(
                1L,
                "YOUNG_FARMER",
                "policy-1",
                "청년농업인 영농정착지원사업",
                "청년농업인의 안정적인 영농 정착을 지원합니다.",
                "청년농업인",
                "전국",
                "정착지원",
                "상시",
                "온라인 신청",
                "농림축산식품부",
                "1234",
                "https://example.com",
                null
        );
    }
}
