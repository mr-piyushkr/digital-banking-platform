package com.bankflow.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bankflow.support.TestFlywayConfig;

/**
 * Asserts the URL-level layer of the authorization model. These are the rules
 * that stop one customer reaching an admin endpoint, so they are worth testing
 * as rules rather than trusting the configuration to be read correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestFlywayConfig.class)
class AuthorizationRulesTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void loginIsPublic() throws Exception {
        // 400 rather than 401 proves the request reached validation, not the wall.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void garbageBearerTokenIsUnauthorizedNotAServerError() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void adminEndpointsRejectAnAnonymousCaller() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    /** The rule that actually matters: a customer must not reach admin routes. */
    @Test
    @WithMockUser(username = "asha@example.com", roles = "CUSTOMER")
    void customerCannotReachAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "auditor@example.com", roles = "AUDITOR")
    void auditorCannotReachAdminWriteEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    /** Read-only monitoring is deliberately shared with the auditor role. */
    @Test
    @WithMockUser(username = "auditor@example.com", roles = "AUDITOR")
    void auditorCanReachAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "asha@example.com", roles = "CUSTOMER")
    void customerCannotReachAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
