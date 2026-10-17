package com.hrgenius.controller;

import com.hrgenius.security.CustomUserDetailsService;
import com.hrgenius.security.JwtAuthenticationFilter;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void testLogoutRevokesTokenAndClearsCookie() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser(username = "alice", roles = "EMPLOYEE")
    void testLogoutAllRevokesAllTokensForUser() throws Exception {
        mockMvc.perform(post("/auth/logout-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out from all devices successfully"));

        verify(authService).logoutAll(eq("alice"), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser(username = "bob", roles = "EMPLOYEE")
    void testChangePasswordCallsService() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldSecret@123\",\"newPassword\":\"NewSecret@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(authService).changePassword(eq("bob"), any(com.hrgenius.dto.auth.ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminResetPasswordCallsService() throws Exception {
        org.mockito.Mockito.when(authService.adminResetPassword(5L)).thenReturn(com.hrgenius.dto.auth.ResetPasswordResponse.builder()
                .userId(5L)
                .username("employee5")
                .temporaryPassword("Temp@12341A")
                .mustChangePassword(true)
                .message("Password reset successfully")
                .build());

        mockMvc.perform(post("/auth/reset-password/5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("Temp@12341A"))
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        verify(authService).adminResetPassword(5L);
    }
}
