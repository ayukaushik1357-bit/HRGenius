package com.hrgenius.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityProfileTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("h2")
    class H2ProfileTests {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void testH2ConsolePermittedInH2Profile() throws Exception {
            // In h2 profile, /h2-console is permitted past security (not rejected with 401/403)
            mockMvc.perform(get("/h2-console"))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(403, result.getResponse().getStatus()));
        }

        @Test
        void testSwaggerPermittedInH2Profile() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }

        @Test
        void testFrameOptionsSameOriginInH2Profile() throws Exception {
            mockMvc.perform(get("/h2-console"))
                    .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "spring.datasource.url=jdbc:h2:mem:prodtest;DB_CLOSE_DELAY=-1;MODE=Oracle",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.flyway.enabled=false",
            "app.jwt.secret=9a3f8b7c1d6e5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b"
    })
    @AutoConfigureMockMvc
    @ActiveProfiles("prod")
    class ProdProfileTests {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void testH2ConsoleDeniedInProdProfile() throws Exception {
            mockMvc.perform(get("/h2-console"))
                    .andExpect(status().isUnauthorized()); // Or denied/unauthorized
        }

        @Test
        void testSwaggerDeniedUnauthenticatedInProdProfile() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        void testSwaggerDeniedForEmployeeInProdProfile() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void testSwaggerAllowedForAdminInProdProfile() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }

        @Test
        void testFrameOptionsDenyInProdProfile() throws Exception {
            mockMvc.perform(get("/auth/login"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }
    }
}
