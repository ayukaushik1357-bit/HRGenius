package com.hrgenius.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.dto.common.PagedResponse;
import com.hrgenius.dto.user.UserRolesRequest;
import com.hrgenius.dto.user.UserResponse;
import com.hrgenius.dto.user.UserStatusRequest;
import com.hrgenius.security.CustomUserDetailsService;
import com.hrgenius.security.JwtAuthenticationFilter;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerTest.MethodSecurityConfig.class)
class UserControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers_Admin_Success() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("admin")
                .email("admin@example.com")
                .enabled(true)
                .roles(Set.of("ADMIN"))
                .build();
        PagedResponse<UserResponse> pagedResponse = PagedResponse.<UserResponse>builder()
                .content(List.of(response))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(userService.getAllUsers(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("admin"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAllUsers_Employee_Forbidden() throws Exception {
        mockMvc.perform(get("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserStatus_Success() throws Exception {
        UserStatusRequest req = new UserStatusRequest(false);
        UserResponse response = UserResponse.builder().id(2L).username("testuser").enabled(false).build();
        when(userService.updateUserStatus(eq(2L), eq(false))).thenReturn(response);

        mockMvc.perform(put("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUnlockUser_Success() throws Exception {
        UserResponse response = UserResponse.builder().id(2L).username("testuser").locked(false).failedAttempts(0).build();
        when(userService.unlockUser(2L)).thenReturn(response);

        mockMvc.perform(post("/users/2/unlock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserRoles_Success() throws Exception {
        UserRolesRequest req = new UserRolesRequest(Set.of("MANAGER"));
        UserResponse response = UserResponse.builder().id(2L).username("testuser").roles(Set.of("MANAGER")).build();
        when(userService.updateUserRoles(eq(2L), anySet())).thenReturn(response);

        mockMvc.perform(put("/users/2/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("MANAGER"));
    }
}
