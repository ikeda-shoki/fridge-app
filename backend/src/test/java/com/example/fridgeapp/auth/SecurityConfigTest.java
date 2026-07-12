package com.example.fridgeapp.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fridgeapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SecurityConfigTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void logoutWithoutCsrfTokenIsForbidden() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isForbidden());
  }

  @Test
  void logoutWithCsrfTokenIsAccepted() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout").with(csrf())).andExpect(status().isNoContent());
  }

  @Test
  void googleLoginIsExemptFromCsrf() throws Exception {
    // CSRF トークン未提供でも 403(CSRF起因) にはならず、バリデーションエラー(400)まで到達すること
    mockMvc
        .perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void allowedOriginPreflightReceivesCorsHeaders() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/logout")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
  }

  @Test
  void disallowedOriginPreflightIsRejected() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/logout")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
  }
}
