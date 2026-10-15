package com.hrgenius.controller;

import com.hrgenius.dto.analytics.AnalyticsDashboardResponse;
import com.hrgenius.security.CustomUserDetailsService;
import com.hrgenius.security.JwtAuthenticationFilter;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void testGetDashboard() throws Exception {
        AnalyticsDashboardResponse response = AnalyticsDashboardResponse.builder()
                .totalHeadcount(50L)
                .activeEmployees(48L)
                .attritionRate(4.0)
                .averageTenureYears(2.5)
                .monthlyPayrollTotal(new BigDecimal("3500000.00"))
                .headcountByDepartment(List.of(AnalyticsDashboardResponse.CategoryCount.builder()
                        .category("Engineering")
                        .count(20L)
                        .build()))
                .build();

        when(analyticsService.getDashboardData(any(), any())).thenReturn(response);

        mockMvc.perform(get("/analytics/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHeadcount").value(50))
                .andExpect(jsonPath("$.activeEmployees").value(48))
                .andExpect(jsonPath("$.attritionRate").value(4.0))
                .andExpect(jsonPath("$.headcountByDepartment[0].category").value("Engineering"));
    }
}
