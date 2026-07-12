package com.example.fridgeapp.group;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fridgeapp.auth.AuthenticatedUser;
import com.example.fridgeapp.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@Transactional
class InvitationControllerTest extends AbstractIntegrationTest {

  private static final AtomicInteger IP_SEQ = new AtomicInteger();

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
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
  void issueInvitationAndJoinAddsMember() throws Exception {
    UUID groupId = createGroup(ownerAuth, "招待テストグループ");
    String code = issueInvitationCode(groupId);

    mockMvc
        .perform(
            post("/api/v1/groups/join")
                .with(authentication(otherAuth))
                .with(csrf())
                .with(uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinGroupRequest(code))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(groupId.toString()));

    mockMvc
        .perform(get("/api/v1/groups/{id}/members", groupId).with(authentication(ownerAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void joinWithInvalidCodeReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/groups/join")
                .with(authentication(otherAuth))
                .with(csrf())
                .with(uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinGroupRequest("ZZZZZZ"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INVITATION_CODE"));
  }

  @Test
  void joinWithAlreadyUsedCodeReturnsBadRequest() throws Exception {
    UUID groupId = createGroup(ownerAuth, "再利用防止グループ");
    String code = issueInvitationCode(groupId);
    join(otherAuth, code);

    UUID thirdUserId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO users (id, google_sub, display_name) VALUES (?, ?, ?)",
        thirdUserId,
        "google-third",
        "三番目次郎");
    Authentication thirdAuth =
        new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(thirdUserId), null, List.of());

    mockMvc
        .perform(
            post("/api/v1/groups/join")
                .with(authentication(thirdAuth))
                .with(csrf())
                .with(uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinGroupRequest(code))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVITATION_CODE_ALREADY_USED"));
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

  private String issueInvitationCode(UUID groupId) throws Exception {
    String responseJson =
        mockMvc
            .perform(
                post("/api/v1/groups/{id}/invitations", groupId)
                    .with(authentication(ownerAuth))
                    .with(csrf()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(responseJson).get("code").asText();
  }

  private void join(Authentication authentication, String code) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/groups/join")
                .with(authentication(authentication))
                .with(csrf())
                .with(uniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinGroupRequest(code))))
        .andExpect(status().isOk());
  }

  /** JoinRateLimiter は IP 単位で状態を保持するシングルトンBeanのため、テストごとに異なる送信元IPを 割り当ててレート制限の影響を受けないようにする。 */
  private RequestPostProcessor uniqueIp() {
    int n = IP_SEQ.incrementAndGet();
    return request -> {
      request.setRemoteAddr("172.16." + (n / 256) + "." + (n % 256));
      return request;
    };
  }
}
