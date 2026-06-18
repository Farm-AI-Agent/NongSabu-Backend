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
        given(policySupportService.search(0, 10, null, null, null, "young"))
                .willReturn(pageResponse());

        mockMvc.perform(get("/api/v1/support-programs")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "young"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("Young Farmer Settlement Support"));

        verify(policySupportService).search(0, 10, null, null, null, "young");
    }

    @Test
    void listUsesPolicyProgramsCompatibilityPath() throws Exception {
        given(policySupportService.search(0, 10, null, null, null, "young"))
                .willReturn(pageResponse());

        mockMvc.perform(get("/api/v1/policy-programs")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "young"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(1));
    }

    @Test
    void recommendedUsesSupportProgramsCompatibilityPath() throws Exception {
        given(policySupportService.recommend(eq(null), any()))
                .willReturn(new PolicyRecommendationResponse(
                        "young farmer",
                        List.of(policyResponse())
                ));

        mockMvc.perform(get("/api/v1/support-programs/recommended")
                        .param("query", "young farmer")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("Young Farmer Settlement Support"));
    }

    @Test
    void detailUsesSupportProgramsCompatibilityPath() throws Exception {
        given(policySupportService.get(1L)).willReturn(policyResponse());

        mockMvc.perform(get("/api/v1/support-programs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Young Farmer Settlement Support"));
    }

    private PolicyPageResponse pageResponse() {
        return new PolicyPageResponse(
                0,
                10,
                1,
                1,
                List.of(policyResponse())
        );
    }

    private PolicySupportResponse policyResponse() {
        return new PolicySupportResponse(
                1L,
                "YOUNG_FARMER",
                "policy-1",
                "Young Farmer Settlement Support",
                "Settlement support for young farmers.",
                "young farmers",
                "nationwide",
                "settlement",
                "always open",
                "online",
                "MAFRA",
                "1234",
                "https://example.com",
                null
        );
    }
}
