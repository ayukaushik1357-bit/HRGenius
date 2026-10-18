package com.hrgenius.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SecurityMatrixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    // --- 1. No token -> 401 Unauthorized ---
    @Test
    @DisplayName("Unauthenticated request to protected endpoints must return 401")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/analytics/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // --- 2. Refresh token used as access token -> 401 Unauthorized ---
    @Test
    @DisplayName("Using a refresh token as Bearer authorization token must return 401")
    void testRefreshTokenUsedAsAccessTokenReturns401() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshTokenFromUsername(
                "admin",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/audit/logs")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // --- 3. Wrong role -> 403 Forbidden ---
    @Test
    @WithMockUser(username = "emp1", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE role accessing ADMIN/HR endpoints must return 403")
    void testEmployeeRoleAccessingAdminEndpointsReturns403() throws Exception {
        // Audit logs: ADMIN, HR_MANAGER
        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // User management: ADMIN only
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Payroll runs: ADMIN, HR_MANAGER
        mockMvc.perform(get("/payroll/runs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Recruitment candidates: ADMIN, HR_MANAGER
        mockMvc.perform(get("/recruitment/candidates"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Onboarding templates: ADMIN, HR_MANAGER
        mockMvc.perform(get("/onboarding/templates"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Holiday management: ADMIN, HR_MANAGER
        mockMvc.perform(post("/attendance/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Holiday\",\"holidayDate\":\"2026-12-25\",\"holidayType\":\"MANDATORY\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "mgr1", roles = {"MANAGER"})
    @DisplayName("MANAGER role accessing ADMIN only user management must return 403")
    void testManagerRoleAccessingAdminOnlyReturns403() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // --- 4. Allowed role -> 2xx Success ---
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN role accessing audit logs must succeed with 200 OK")
    void testAdminRoleAccessingAuditLogsReturns200() throws Exception {
        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN role accessing user list must succeed with 200 OK")
    void testAdminRoleAccessingUserManagementReturns200() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "hr1", roles = {"HR_MANAGER"})
    @DisplayName("HR_MANAGER accessing recruitment candidates must succeed with 200 OK")
    void testHrManagerAccessingRecruitmentReturns200() throws Exception {
        mockMvc.perform(get("/recruitment/candidates"))
                .andExpect(status().isOk());
    }
}
