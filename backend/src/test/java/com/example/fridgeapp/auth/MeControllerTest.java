package com.example.fridgeapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fridgeapp.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class MeControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserRepository userRepository;

  private UUID userId;
  private Authentication userAuth;

  @BeforeEach
  void seedUser() {
    userId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO users (id, google_sub, display_name) VALUES (?, ?, ?)",
        userId,
        "google-sub",
        "テスト太郎");
    userAuth =
        new UsernamePasswordAuthenticationToken(new AuthenticatedUser(userId), null, List.of());
  }

  @Test
  void deleteMeMarksUserDeletedAndClearsCookies() throws Exception {
    mockMvc
        .perform(delete("/api/v1/me").with(authentication(userAuth)).with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("access_token=")))
        .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")))
        .andReturn();

    assertThat(userRepository.findById(userId)).get().extracting(User::isDeleted).isEqualTo(true);
  }

  @Test
  void deleteMeRejectsSoleOwnerOfGroup() throws Exception {
    UUID groupId = UUID.randomUUID();
    jdbcTemplate.update("INSERT INTO groups (id, name) VALUES (?, ?)", groupId, "テスト家族");
    jdbcTemplate.update(
        "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'OWNER')",
        groupId,
        userId);

    mockMvc
        .perform(delete("/api/v1/me").with(authentication(userAuth)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ACCOUNT_HAS_SOLE_OWNERSHIP"));

    assertThat(userRepository.findById(userId)).get().extracting(User::isDeleted).isEqualTo(false);
  }

  @Test
  void deleteMeSucceedsWhenAnotherOwnerExists() throws Exception {
    UUID groupId = UUID.randomUUID();
    UUID otherOwnerId = UUID.randomUUID();
    jdbcTemplate.update("INSERT INTO groups (id, name) VALUES (?, ?)", groupId, "テスト家族");
    jdbcTemplate.update(
        "INSERT INTO users (id, google_sub, display_name) VALUES (?, ?, ?)",
        otherOwnerId,
        "google-sub-2",
        "テスト次郎");
    jdbcTemplate.update(
        "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'OWNER')",
        groupId,
        userId);
    jdbcTemplate.update(
        "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'OWNER')",
        groupId,
        otherOwnerId);

    mockMvc
        .perform(delete("/api/v1/me").with(authentication(userAuth)).with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void getMeReturnsUserInfoWithGroups() throws Exception {
    UUID groupId = UUID.randomUUID();
    jdbcTemplate.update("INSERT INTO groups (id, name) VALUES (?, ?)", groupId, "我が家");
    jdbcTemplate.update(
        "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'OWNER')",
        groupId,
        userId);

    mockMvc
        .perform(get("/api/v1/me").with(authentication(userAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("テスト太郎"))
        .andExpect(jsonPath("$.groups", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$.groups[0].id").value(groupId.toString()))
        .andExpect(jsonPath("$.groups[0].name").value("我が家"));
  }

  @Test
  void getMeReturnsEmptyGroupsWhenUserHasNoGroup() throws Exception {
    mockMvc
        .perform(get("/api/v1/me").with(authentication(userAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groups", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  void deletedUserCannotAccessMeAnymore() throws Exception {
    mockMvc
        .perform(delete("/api/v1/me").with(authentication(userAuth)).with(csrf()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/me").with(authentication(userAuth)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  void deleteMeRequiresAuthentication() throws Exception {
    mockMvc.perform(delete("/api/v1/me").with(csrf())).andExpect(status().isUnauthorized());
  }
}
