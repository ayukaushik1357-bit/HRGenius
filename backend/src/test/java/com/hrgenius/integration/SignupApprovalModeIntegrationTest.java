package com.hrgenius.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.entity.User;
import com.hrgenius.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Optional settings: sign-ups that need administrator approval, and ADMIN_PASSWORD for the admin account.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = {
        "app.account.signup-requires-approval=true",
        "app.account.admin-password=" + SignupApprovalModeIntegrationTest.ADMIN_PASSWORD
})
class SignupApprovalModeIntegrationTest {

    static final String ADMIN_PASSWORD = "Sup3r!Secret";
    private static final String STRONG = "Str0ng!Pass";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("ADMIN_PASSWORD replaces the default admin password")
    void adminPasswordComesFromSetting() throws Exception {
        login("admin", "Admin@123", "10.1.0.1").andExpect(status().isUnauthorized());
        login("admin", ADMIN_PASSWORD, "10.1.0.1").andExpect(status().isOk());
    }

    @Test
    @DisplayName("With approval on, a sign-up waits for an admin; the approved account has no data until HR links it")
    void signupWaitsForApproval() throws Exception {
        String email = "newjoiner." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        register(email, "10.1.0.2").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        login(email, STRONG, "10.1.0.2").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("approval")));
        login(email, "Wrong!Pass1", "10.1.0.2").andExpect(status().isUnauthorized());

        User pending = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(pending.isPendingApproval()).isTrue();
        mockMvc.perform(put("/users/" + pending.getId() + "/status")
                        .header("Authorization", "Bearer " + accessToken("admin", ADMIN_PASSWORD, "10.1.0.3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApproval").value(false));

        String token = accessToken(email, STRONG, "10.1.0.2");
        // No employee record yet: a clear 404, never another employee's data
        mockMvc.perform(get("/portal/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not linked")));
        mockMvc.perform(post("/portal/profile-update").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedChanges\":\"{\\\"phone\\\":\\\"+91 9000000000\\\"}\"}"))
                .andExpect(status().isNotFound());
    }

    private static RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private ResultActions login(String identifier, String password, String clientIp) throws Exception {
        return mockMvc.perform(post("/auth/login").with(ip(clientIp))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", identifier, "password", password))));
    }

    private ResultActions register(String email, String clientIp) throws Exception {
        return mockMvc.perform(post("/auth/register").with(ip(clientIp))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "firstName", "Test", "lastName", "User", "email", email, "password", STRONG))));
    }

    private String accessToken(String user, String password, String clientIp) throws Exception {
        String body = login(user, password, clientIp).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }
}
