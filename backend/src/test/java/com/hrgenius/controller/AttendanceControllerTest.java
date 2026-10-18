package com.hrgenius.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.dto.attendance.AttendanceResponse;
import com.hrgenius.security.CurrentUserService;
import com.hrgenius.security.CustomUserDetailsService;
import com.hrgenius.security.JwtAuthenticationFilter;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.service.AttendanceService;
import com.hrgenius.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceService attendanceService;

    @MockBean
    private HolidayService holidayService;

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
    void testCheckIn_OtherEmployeeId_Forbidden() throws Exception {
        when(currentUserService.getEmployeeIdForUser(any(), eq(999L)))
                .thenThrow(new AccessDeniedException("Access denied: You can only act on your own employee profile"));

        mockMvc.perform(post("/attendance/check-in")
                        .param("employeeId", "999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYEE"})
    void testGetTodayAttendance_OtherEmployeeId_Forbidden() throws Exception {
        when(currentUserService.getEmployeeIdForUser(any(), eq(999L)))
                .thenThrow(new AccessDeniedException("Access denied: You can only act on your own employee profile"));

        mockMvc.perform(get("/attendance/today")
                        .param("employeeId", "999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYEE"})
    void testGetTodayAttendance_OwnId_Success() throws Exception {
        when(currentUserService.getEmployeeIdForUser(any(), eq(10L))).thenReturn(10L);
        when(attendanceService.getTodayAttendance(10L)).thenReturn(AttendanceResponse.builder().id(1L).build());

        mockMvc.perform(get("/attendance/today")
                        .param("employeeId", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    void testCheckIn_ManagerForDirectReport_Success() throws Exception {
        when(currentUserService.getEmployeeIdForUser(any(), eq(300L))).thenReturn(300L);
        when(attendanceService.checkIn(eq(300L), any())).thenReturn(AttendanceResponse.builder().id(2L).build());

        mockMvc.perform(post("/attendance/check-in")
                        .param("employeeId", "300"))
                .andExpect(status().isOk());
    }
}
