package com.example.fridgeapp.fridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fridgeapp.auth.AuthenticatedUser;
import com.example.fridgeapp.support.AbstractIntegrationTest;
import java.time.LocalDate;
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
class FridgeItemControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ConsumptionEventRepository consumptionEventRepository;

  private UUID groupId;
  private Authentication memberAuth;
  private Authentication outsiderAuth;

  @BeforeEach
  void seedGroupAndMembers() {
    groupId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();

    jdbcTemplate.update("INSERT INTO groups (id, name) VALUES (?, ?)", groupId, "テスト家族");
    jdbcTemplate.update(
        "INSERT INTO users (id, google_sub, display_name) VALUES (?, ?, ?)",
        memberId,
        "google-member",
        "メンバー太郎");
    jdbcTemplate.update(
        "INSERT INTO users (id, google_sub, display_name) VALUES (?, ?, ?)",
        outsiderId,
        "google-outsider",
        "部外者花子");
    jdbcTemplate.update(
        "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'OWNER')",
        groupId,
        memberId);

    memberAuth =
        new UsernamePasswordAuthenticationToken(new AuthenticatedUser(memberId), null, List.of());
    outsiderAuth =
        new UsernamePasswordAuthenticationToken(new AuthenticatedUser(outsiderId), null, List.of());
  }

  @Test
  void createItemStoresItemAndReturnsIt() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/fridge-items", groupId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName":"玉ねぎ","quantity":2,"unit":"個","category":"野菜",
                     "expiresAt":"2026-08-01","memo":"カレー用"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("玉ねぎ"))
        .andExpect(jsonPath("$.category").value("野菜"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.purchasedAt").value(LocalDate.now().toString()));
  }

  @Test
  void createItemRejectsUnknownCategory() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/fridge-items", groupId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"宇宙食\",\"quantity\":1,\"category\":\"宇宙\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_FRIDGE_ITEM_CATEGORY"));
  }

  @Test
  void nonMemberCannotCreateItem() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/fridge-items", groupId)
                .with(authentication(outsiderAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"玉ねぎ\",\"quantity\":1,\"category\":\"野菜\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
  }

  @Test
  void listItemsOrdersByExpiryWithUndatedItemsLast() throws Exception {
    createItem("期限なし", "野菜", null);
    createItem("期限遠い", "野菜", "2026-12-31");
    createItem("期限近い", "野菜", "2026-07-20");

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}/fridge-items", groupId).with(authentication(memberAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].displayName").value("期限近い"))
        .andExpect(jsonPath("$[1].displayName").value("期限遠い"))
        .andExpect(jsonPath("$[2].displayName").value("期限なし"));
  }

  @Test
  void listItemsFiltersByCategoryAndNameTogether() throws Exception {
    createItem("玉ねぎ", "野菜", null);
    createItem("玉子", "乳製品", null);
    createItem("人参", "野菜", null);

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}/fridge-items", groupId)
                .param("category", "野菜")
                .param("q", "玉")
                .with(authentication(memberAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].displayName").value("玉ねぎ"));
  }

  @Test
  void listItemsExcludesDeletedAndConsumedItems() throws Exception {
    UUID deletedId = createItem("削除済み", "野菜", null);
    UUID consumedId = createItem("消費済み", "野菜", null);
    createItem("残っている", "野菜", null);

    mockMvc
        .perform(
            delete("/api/v1/fridge-items/{id}", deletedId)
                .with(authentication(memberAuth))
                .with(csrf()))
        .andExpect(status().isNoContent());
    consume(consumedId, 1);

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}/fridge-items", groupId).with(authentication(memberAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].displayName").value("残っている"));
  }

  @Test
  void updateItemChangesOnlyProvidedFields() throws Exception {
    UUID itemId = createItem("玉ねぎ", "野菜", "2026-08-01");

    mockMvc
        .perform(
            patch("/api/v1/fridge-items/{id}", itemId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(5))
        .andExpect(jsonPath("$.displayName").value("玉ねぎ"))
        .andExpect(jsonPath("$.expiresAt").value("2026-08-01"));
  }

  @Test
  void consumeItemRecordsConsumptionEventAndMarksConsumedAtZero() throws Exception {
    UUID itemId = createItem("玉ねぎ", "野菜", null);

    mockMvc
        .perform(
            post("/api/v1/fridge-items/{id}/consume", itemId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(0))
        .andExpect(jsonPath("$.status").value("CONSUMED"));

    List<ConsumptionEvent> events = consumptionEventRepository.findAll();
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getFridgeItemId()).isEqualTo(itemId);
    assertThat(events.get(0).getReason()).isEqualTo(ConsumptionReason.MANUAL);
    assertThat(events.get(0).getQuantityConsumed()).isEqualByComparingTo("1");
  }

  @Test
  void consumeItemRejectsQuantityLargerThanRemaining() throws Exception {
    UUID itemId = createItem("玉ねぎ", "野菜", null);

    mockMvc
        .perform(
            post("/api/v1/fridge-items/{id}/consume", itemId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":99}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_QUANTITY"));
  }

  @Test
  void deletedItemCannotBeUpdatedAgain() throws Exception {
    UUID itemId = createItem("玉ねぎ", "野菜", null);

    mockMvc
        .perform(
            delete("/api/v1/fridge-items/{id}", itemId)
                .with(authentication(memberAuth))
                .with(csrf()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            patch("/api/v1/fridge-items/{id}", itemId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FRIDGE_ITEM_NOT_ACTIVE"));
  }

  @Test
  void nonMemberCannotReadOrModifyItems() throws Exception {
    UUID itemId = createItem("玉ねぎ", "野菜", null);

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}/fridge-items", groupId)
                .with(authentication(outsiderAuth)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/v1/fridge-items/{id}", itemId)
                .with(authentication(outsiderAuth))
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
  }

  @Test
  void listItemsRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/groups/{groupId}/fridge-items", groupId))
        .andExpect(status().isUnauthorized());
  }

  private UUID createItem(String displayName, String category, String expiresAt) throws Exception {
    String body =
        objectMapper.writeValueAsString(
            new FridgeItemCreateRequest(
                null,
                displayName,
                new java.math.BigDecimal("1"),
                "個",
                category,
                expiresAt == null ? null : LocalDate.parse(expiresAt),
                null,
                null,
                null));
    String json =
        mockMvc
            .perform(
                post("/api/v1/groups/{groupId}/fridge-items", groupId)
                    .with(authentication(memberAuth))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }

  private void consume(UUID itemId, int quantity) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/fridge-items/{id}/consume", itemId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":" + quantity + "}"))
        .andExpect(status().isOk());
  }
}
