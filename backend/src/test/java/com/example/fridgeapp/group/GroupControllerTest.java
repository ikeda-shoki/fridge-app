package com.example.fridgeapp.group;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fridgeapp.auth.AuthenticatedUser;
import com.example.fridgeapp.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@Transactional
class GroupControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private GroupMemberRepository groupMemberRepository;
  @Autowired private ObjectMapper objectMapper;

  private UUID ownerId;
  private UUID otherUserId;
  private Authentication ownerAuth;
  private Authentication otherAuth;

  @BeforeEach
  void seedUsers() {
    ownerId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO users (id, google_sub, display_name) VALUES (?, ?, ?)",
        ownerId,
        "google-owner",
        "オーナー太郎");
    jdbcTemplate.update(
        "INSERT INTO users (id, google_sub, display_name) VALUES (?, ?, ?)",
        otherUserId,
        "google-other",
        "他ユーザー花子");
    ownerAuth =
        new UsernamePasswordAuthenticationToken(new AuthenticatedUser(ownerId), null, List.of());
    otherAuth =
        new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(otherUserId), null, List.of());
  }

  @Test
  void createGroupMakesCreatorOwner() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/groups")
                .with(authentication(ownerAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"我が家\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("我が家"));
  }

  @Test
  void nonMemberCannotViewGroupDetail() throws Exception {
    UUID groupId = createGroup(ownerAuth, "非公開グループ");

    mockMvc
        .perform(get("/api/v1/groups/{id}", groupId).with(authentication(otherAuth)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
  }

  @Test
  void memberCanViewGroupDetail() throws Exception {
    UUID groupId = createGroup(ownerAuth, "見えるグループ");

    mockMvc
        .perform(get("/api/v1/groups/{id}", groupId).with(authentication(ownerAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("見えるグループ"));
  }

  @Test
  void soleOwnerCannotLeaveGroup() throws Exception {
    UUID groupId = createGroup(ownerAuth, "唯一オーナーグループ");

    mockMvc
        .perform(
            delete("/api/v1/groups/{id}/members/me", groupId)
                .with(authentication(ownerAuth))
                .with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LAST_OWNER_CANNOT_LEAVE"));
  }

  @Test
  void transferOwnershipAllowsPreviousOwnerToLeave() throws Exception {
    UUID groupId = createGroup(ownerAuth, "譲渡テストグループ");
    addMember(groupId, otherUserId);

    mockMvc
        .perform(
            post("/api/v1/groups/{id}/members/{userId}/transfer-ownership", groupId, otherUserId)
                .with(authentication(ownerAuth))
                .with(csrf()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            delete("/api/v1/groups/{id}/members/me", groupId)
                .with(authentication(ownerAuth))
                .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void nonOwnerCannotDeleteGroup() throws Exception {
    UUID groupId = createGroup(ownerAuth, "削除禁止グループ");
    addMember(groupId, otherUserId);

    mockMvc
        .perform(
            delete("/api/v1/groups/{id}", groupId).with(authentication(otherAuth)).with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_GROUP_OWNER"));
  }

  @Test
  void ownerCanDeleteGroup() throws Exception {
    UUID groupId = createGroup(ownerAuth, "削除対象グループ");

    mockMvc
        .perform(
            delete("/api/v1/groups/{id}", groupId).with(authentication(ownerAuth)).with(csrf()))
        .andExpect(status().isNoContent());
  }

  private UUID createGroup(Authentication authentication, String name) throws Exception {
    String responseJson =
        mockMvc
            .perform(
                post("/api/v1/groups")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new GroupCreateRequest(name))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());
  }

  /** 招待コード機能(InvitationService)に依存せず、直接 MEMBER として追加する。 */
  private void addMember(UUID groupId, UUID userId) {
    groupMemberRepository.save(new GroupMember(groupId, userId, GroupRole.MEMBER, null));
  }
}
