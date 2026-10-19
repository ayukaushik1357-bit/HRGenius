package com.hrgenius.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.entity.User;
import com.hrgenius.repository.UserRepository;
import com.hrgenius.security.GoogleTokenVerifier;
import com.hrgenius.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sign-up with approval, email login, lockout, password reset and Google sign-in against the real
 * (H2) database. Only the Google token check and outgoing email are mocked.
 * Each test uses its own client IP so the per-IP rate limiter does not interfere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AccountFlowIntegrationTest {

    private static final String STRONG = "Str0ng!Pass";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private com.hrgenius.repository.EmployeeRepository employeeRepository;

    @MockBean private GoogleTokenVerifier googleTokenVerifier;
    @MockBean private NotificationService notificationService;

    @Test
    @DisplayName("Login works with email address as well as username")
    void loginWithEmail() throws Exception {
        login("employee@hrgenius.com", "Employee@123", "10.0.1.1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        login("EMPLOYEE@HRGENIUS.COM", "Employee@123", "10.0.1.1").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unknown username or email gets the same 401 as a wrong password")
    void unknownUserGetsGeneric401() throws Exception {
        login("nobody_" + UUID.randomUUID().toString().substring(0, 6), "Wrong!Pass1", "10.0.1.2")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        login(unique("ghost"), "Wrong!Pass1", "10.0.1.2")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sign-up stays blocked until an admin approves it, then the user can sign in")
    void signupNeedsApproval() throws Exception {
        String email = unique("newjoiner");
        register(email, STRONG, "10.0.2.1").andExpect(status().isCreated());

        // Correct password -> clear "pending approval" message; wrong password -> generic 401
        login(email, STRONG, "10.0.2.1").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("approval")));
        login(email, "Wrong!Pass1", "10.0.2.1").andExpect(status().isUnauthorized());

        // Duplicate sign-up is rejected
        register(email, STRONG, "10.0.2.1").andExpect(status().isConflict());

        // Admin approves in User Management
        User pending = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(pending.isPendingApproval()).isTrue();
        assertThat(pending.isEnabled()).isFalse();
        mockMvc.perform(put("/users/" + pending.getId() + "/status")
                        .header("Authorization", "Bearer " + accessToken("admin", "Admin@123", "10.0.2.2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApproval").value(false));

        login(email, STRONG, "10.0.2.1").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sign-up rejects weak passwords and invalid emails")
    void signupValidation() throws Exception {
        register(unique("weak"), "password", "10.0.3.1").andExpect(status().isBadRequest());
        register("not-an-email", STRONG, "10.0.3.1").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Five wrong passwords lock the account even though the login request fails")
    void lockoutIsPersisted() throws Exception {
        String email = approvedUser("locky", "10.0.4.1");
        for (int i = 0; i < 5; i++) {
            login(email, "Wrong!Pass1", "10.0.4.1").andExpect(status().isUnauthorized());
        }
        // Correct password is now refused because the account is locked
        login(email, STRONG, "10.0.4.1").andExpect(status().isLocked());
    }

    @Test
    @DisplayName("Password reset: generic answer, one-time link, old password stops working")
    void passwordReset() throws Exception {
        String email = approvedUser("forgetful", "10.0.5.1");

        // Unknown email gets the same answer and no email is sent
        String unknown = "nobody-" + UUID.randomUUID() + "@example.com";
        requestReset(unknown, "10.0.5.1").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("If an account exists")));
        verify(notificationService, never()).sendEmailNotification(eq(unknown), anyString(), anyString());

        requestReset(email, "10.0.5.1").andExpect(status().isOk());
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendEmailNotification(eq(email), anyString(), body.capture());
        Matcher m = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body.getValue());
        assertThat(m.find()).isTrue();
        String token = m.group(1);

        confirmReset(token, "weak").andExpect(status().isBadRequest());
        confirmReset(token, "N3w!Password").andExpect(status().isOk());
        confirmReset(token, "An0ther!Pass").andExpect(status().isBadRequest()); // one-time use

        login(email, STRONG, "10.0.5.2").andExpect(status().isUnauthorized());
        login(email, "N3w!Password", "10.0.5.2").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Google: existing account signs in, unknown account becomes a pending sign-up, bad token is rejected")
    void googleSignIn() throws Exception {
        when(googleTokenVerifier.verify("good-existing"))
                .thenReturn(new GoogleTokenVerifier.GoogleIdentity("manager@hrgenius.com", "Vikram", "Malhotra"));
        String newEmail = unique("googler");
        when(googleTokenVerifier.verify("good-new"))
                .thenReturn(new GoogleTokenVerifier.GoogleIdentity(newEmail, "New", "Person"));
        when(googleTokenVerifier.verify("forged")).thenThrow(new BadCredentialsException("Invalid Google credential"));

        google("good-existing", "10.0.6.1").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED_IN"))
                .andExpect(jsonPath("$.auth.username").value("manager"))
                .andExpect(jsonPath("$.auth.accessToken").isNotEmpty());

        google("good-new", "10.0.6.1").andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        assertThat(userRepository.findByEmailIgnoreCase(newEmail)).get()
                .extracting(User::isPendingApproval).isEqualTo(true);
        google("good-new", "10.0.6.1").andExpect(status().isForbidden());

        google("forged", "10.0.6.1").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A user with no employee record gets 404 on the portal, never another employee's data")
    void portalWithoutEmployeeRecordDoesNotLeakData() throws Exception {
        String email = approvedUser("unlinked", "10.0.7.1");
        String token = accessToken(email, STRONG, "10.0.7.1");

        mockMvc.perform(get("/portal/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not linked")));
        mockMvc.perform(post("/portal/profile-update").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedChanges\":\"{\\\"phone\\\":\\\"+91 9000000000\\\"}\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("An employee cannot file a resignation in someone else's name")
    void resignationForAnotherEmployeeIsForbidden() throws Exception {
        String token = accessToken("employee", "Employee@123", "10.0.8.1");
        Long colleagueId = employeeRepository.findByEmployeeCode("EMP005").orElseThrow().getId();

        mockMvc.perform(post("/offboarding/resign").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":" + colleagueId + ",\"resignationDate\":\"2026-11-01\","
                                + "\"lastWorkingDay\":\"2026-12-01\",\"reason\":\"Not mine to file\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public config tells the login page what to show")
    void publicConfig() throws Exception {
        mockMvc.perform(get("/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signupEnabled").value(true))
                .andExpect(jsonPath("$.googleEnabled").value(false));
    }

    // ---------------------------------------------------------------- helpers

    private static RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static String unique(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private ResultActions login(String identifier, String password, String clientIp) throws Exception {
        return mockMvc.perform(post("/auth/login").with(ip(clientIp))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("username", identifier, "password", password))));
    }

    private ResultActions register(String email, String password, String clientIp) throws Exception {
        return mockMvc.perform(post("/auth/register").with(ip(clientIp))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "firstName", "Test", "lastName", "User", "email", email, "password", password))));
    }

    private ResultActions requestReset(String email, String clientIp) throws Exception {
        return mockMvc.perform(post("/auth/password-reset/request").with(ip(clientIp))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("email", email))));
    }

    private ResultActions confirmReset(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("token", token, "newPassword", newPassword))));
    }

    private ResultActions google(String credential, String clientIp) throws Exception {
        return mockMvc.perform(post("/auth/google").with(ip(clientIp))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("credential", credential))));
    }

    private String accessToken(String user, String password, String clientIp) throws Exception {
        MvcResult result = login(user, password, clientIp).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    /** Registers a user and approves it directly in the database. */
    private String approvedUser(String prefix, String clientIp) throws Exception {
        String email = unique(prefix);
        register(email, STRONG, clientIp).andExpect(status().isCreated());
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setEnabled(true);
        user.setPendingApproval(false);
        userRepository.save(user);
        return email;
    }
}
