package com.hrgenius.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.dto.leave.LeaveApplicationRequest;
import com.hrgenius.dto.leave.LeaveApplicationResponse;
import com.hrgenius.security.CurrentUserService;
import com.hrgenius.security.CustomUserDetailsService;
import com.hrgenius.security.JwtAuthenticationFilter;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.service.LeaveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveService leaveService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYEE"})
    void testGetMyLeaves_OwnId_Success() throws Exception {
        when(currentUserService.getEmployeeIdForUser(any(), eq(10L))).thenReturn(10L);
        when(leaveService.getEmployeeLeaves(10L)).thenReturn(List.of());

        mockMvc.perform(get("/leaves/my")
                        .param("employeeId", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYEE"})
    void testGetMyLeaves_OtherEmployeeId_Forbidden() throws Exception {
        when(currentUserService.getEmployeeIdForUser(any(), eq(999L)))
                .thenThrow(new AccessDeniedException("Access denied: You can only act on your own employee profile"));

        mockMvc.perform(get("/leaves/my")
                        .param("employeeId", "999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYEE"})
    void testApplyLeave_OtherEmployeeId_Forbidden() throws Exception {
        when(currentUserService.getEmployeeIdForUser(any(), eq(999L)))
                .thenThrow(new AccessDeniedException("Access denied: You can only act on your own employee profile"));

        LeaveApplicationRequest req = new LeaveApplicationRequest();
        req.setLeaveType("CASUAL");
        req.setStartDate(LocalDate.now().plusDays(1));
        req.setEndDate(LocalDate.now().plusDays(2));
        req.setNumberOfDays(2.0);
        req.setReason("Personal work");

        mockMvc.perform(post("/leaves")
                        .param("employeeId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testApplyLeave_AdminForAnyEmployee_Success() throws Exception {
        when(currentUserService.getEmployeeIdForUser(any(), eq(999L))).thenReturn(999L);
        when(leaveService.applyLeave(eq(999L), any())).thenReturn(LeaveApplicationResponse.builder().id(1L).build());

        LeaveApplicationRequest req = new LeaveApplicationRequest();
        req.setLeaveType("CASUAL");
        req.setStartDate(LocalDate.now().plusDays(1));
        req.setEndDate(LocalDate.now().plusDays(2));
        req.setNumberOfDays(2.0);
        req.setReason("Personal work");

        mockMvc.perform(post("/leaves")
                        .param("employeeId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
