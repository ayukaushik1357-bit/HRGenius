package com.hrgenius.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.dto.selfservice.PortalDashboardResponse;
import com.hrgenius.dto.selfservice.ProfileUpdateRequestDto;
import com.hrgenius.security.CustomUserDetailsService;
import com.hrgenius.security.JwtAuthenticationFilter;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.service.SelfServicePortalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SelfServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class SelfServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SelfServicePortalService selfServicePortalService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "emp.user")
    void testGetPortalDashboard() throws Exception {
        EmployeeResponse profile = EmployeeResponse.builder()
                .id(5L)
                .employeeCode("EMP005")
                .firstName("Aarav")
                .lastName("Sharma")
                .email("aarav.sharma@hrgenius.internal")
                .build();

        PortalDashboardResponse dashboard = PortalDashboardResponse.builder()
                .profile(profile)
                .upcomingHolidays(Collections.emptyList())
                .pendingTasksCount(2)
                .unreadNotificationsCount(1)
                .build();

        when(selfServicePortalService.getPortalDashboard()).thenReturn(dashboard);

        mockMvc.perform(get("/portal/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.firstName").value("Aarav"))
                .andExpect(jsonPath("$.profile.employeeCode").value("EMP005"))
                .andExpect(jsonPath("$.pendingTasksCount").value(2));
    }

    @Test
    @WithMockUser(username = "emp.user")
    void testSubmitProfileUpdateRequest() throws Exception {
        ProfileUpdateRequestDto.Request request = new ProfileUpdateRequestDto.Request();
        request.setRequestedChanges("{\"phone\":\"9876543210\"}");

        ProfileUpdateRequestDto.Response dto = ProfileUpdateRequestDto.Response.builder()
                .id(1L)
                .employeeId(5L)
                .employeeName("Aarav Sharma")
                .status("PENDING")
                .build();

        when(selfServicePortalService.submitProfileUpdateRequest(any())).thenReturn(dto);

        mockMvc.perform(post("/portal/profile-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
