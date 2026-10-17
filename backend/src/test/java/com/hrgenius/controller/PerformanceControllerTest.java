package com.hrgenius.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.dto.performance.GoalRequest;
import com.hrgenius.dto.performance.GoalResponse;
import com.hrgenius.dto.performance.ReviewCycleResponse;
import com.hrgenius.security.CustomUserDetailsService;
import com.hrgenius.security.JwtAuthenticationFilter;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.service.PerformanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PerformanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PerformanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PerformanceService performanceService;

    @MockBean
    private com.hrgenius.security.CurrentUserService currentUserService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    void testGetAllCycles() throws Exception {
        ReviewCycleResponse cycle = ReviewCycleResponse.builder()
                .id(1L)
                .name("Q4 2026 Annual Review")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .status("ACTIVE")
                .build();

        when(performanceService.getAllCycles()).thenReturn(List.of(cycle));

        mockMvc.perform(get("/performance/cycles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Q4 2026 Annual Review"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    void testCreateGoal() throws Exception {
        GoalRequest request = new GoalRequest();
        request.setEmployeeId(1L);
        request.setReviewCycleId(1L);
        request.setTitle("Deliver Spring Boot Microservices");
        request.setDescription("Complete performance module");
        request.setWeightage(30.0);
        request.setProgressPercentage(0.0);

        GoalResponse response = GoalResponse.builder()
                .id(10L)
                .title("Deliver Spring Boot Microservices")
                .weightage(30.0)
                .progressPercentage(0.0)
                .build();

        when(performanceService.createGoal(any(GoalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/performance/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Deliver Spring Boot Microservices"));
    }
}
