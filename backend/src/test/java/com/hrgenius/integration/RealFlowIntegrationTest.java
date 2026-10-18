package com.hrgenius.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks against the real (H2) database and seeded demo data, without mocks.
 * These catch bugs that mocked unit tests cannot (transaction rollbacks, real query result types).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class RealFlowIntegrationTest {

    private static final String REFRESH_COOKIE = "hrgenius_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Replaying a stolen refresh token after the chain moved on revokes every session")
    void refreshTokenReuseRevokesAllSessions() throws Exception {
        Cookie stolen = loginAndGetRefreshCookie("hrmanager", "Hr@123");

        // Legitimate user keeps using the app: stolen -> second -> current
        Cookie second = refresh(stolen);
        Cookie current = refresh(second);

        // Attacker replays the old token -> rejected
        mockMvc.perform(post("/auth/refresh").cookie(stolen))
                .andExpect(status().isBadRequest());

        // ...and the legitimate current token must now be revoked as well
        mockMvc.perform(post("/auth/refresh").cookie(current))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Fast reload / two tabs sending the same refresh cookie does not log the user out")
    void concurrentRefreshWithinGraceWindowIsAllowed() throws Exception {
        Cookie original = loginAndGetRefreshCookie("manager", "Manager@123");

        Cookie fromFirstTab = refresh(original);
        // Second tab still holds the original cookie and refreshes a moment later
        Cookie fromSecondTab = refresh(original);

        // Both tabs keep working
        refresh(fromFirstTab);
        refresh(fromSecondTab);
    }

    private Cookie loginAndGetRefreshCookie(String username, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = login.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private Cookie refresh(Cookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        Cookie next = result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(next).isNotNull();
        assertThat(next.getValue()).isNotEqualTo(cookie.getValue());
        return next;
    }

    @Test
    @DisplayName("Analytics dashboard works on real seeded data (rating distribution query)")
    void analyticsDashboardLoadsWithRealData() throws Exception {
        String token = loginAndGetAccessToken("admin", "Admin@123");

        mockMvc.perform(get("/analytics/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/export/excel").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Missing required query parameter returns 400, not 500")
    void missingParameterIsBadRequest() throws Exception {
        String token = loginAndGetAccessToken("manager", "Manager@123");

        mockMvc.perform(get("/performance/reviews/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
