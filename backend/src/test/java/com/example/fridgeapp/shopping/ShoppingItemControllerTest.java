package com.example.fridgeapp.shopping;

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
import com.example.fridgeapp.fridge.FridgeItemRepository;
import com.example.fridgeapp.support.AbstractIntegrationTest;
import java.math.BigDecimal;
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
class ShoppingItemControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private FridgeItemRepository fridgeItemRepository;

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
            post("/api/v1/groups/{groupId}/shopping-items", groupId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"牛乳\",\"quantity\":1,\"memo\":\"特売\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("牛乳"))
        .andExpect(jsonPath("$.memo").value("特売"))
        .andExpect(jsonPath("$.checked").value(false));
  }

  @Test
  void nonMemberCannotCreateItem() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/shopping-items", groupId)
                .with(authentication(outsiderAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"牛乳\",\"quantity\":1}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
  }

  @Test
  void listItemsReturnsItemsInRegistrationOrder() throws Exception {
    createItem("牛乳", 1);
    createItem("卵", 1);

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}/shopping-items", groupId)
                .with(authentication(memberAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].displayName").value("牛乳"))
        .andExpect(jsonPath("$[1].displayName").value("卵"));
  }

  @Test
  void updateItemChecksItemWithoutChangingOtherFields() throws Exception {
    UUID itemId = createItem("牛乳", 1);

    mockMvc
        .perform(
            patch("/api/v1/shopping-items/{id}", itemId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checked\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.checked").value(true))
        .andExpect(jsonPath("$.displayName").value("牛乳"));
  }

  @Test
  void deleteItemRemovesItFromList() throws Exception {
    UUID itemId = createItem("牛乳", 1);

    mockMvc
        .perform(
            delete("/api/v1/shopping-items/{id}", itemId)
                .with(authentication(memberAuth))
                .with(csrf()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}/shopping-items", groupId)
                .with(authentication(memberAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void moveToFridgeRejectsUncheckedItem() throws Exception {
    UUID itemId = createItem("牛乳", 1);

    mockMvc
        .perform(
            post("/api/v1/shopping-items/{id}/move-to-fridge", itemId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"乳製品\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SHOPPING_ITEM_NOT_CHECKED"));
  }

  @Test
  void moveToFridgeCreatesFridgeItemAndRemovesFromShoppingList() throws Exception {
    UUID itemId = createItem("牛乳", 2);
    checkItem(itemId);

    String json =
        mockMvc
            .perform(
                post("/api/v1/shopping-items/{id}/move-to-fridge", itemId)
                    .with(authentication(memberAuth))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":\"乳製品\",\"expiresAt\":\"2026-08-01\",\"unit\":\"本\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("牛乳"))
            .andExpect(jsonPath("$.category").value("乳製品"))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID fridgeItemId = UUID.fromString(objectMapper.readTree(json).get("id").asText());
    assertThat(fridgeItemRepository.findById(fridgeItemId)).isPresent();

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}/shopping-items", groupId)
                .with(authentication(memberAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void clearCheckedItemsRemovesOnlyCheckedItems() throws Exception {
    UUID checkedId = createItem("牛乳", 1);
    createItem("卵", 1);
    checkItem(checkedId);

    mockMvc
        .perform(
            delete("/api/v1/groups/{groupId}/shopping-items/checked", groupId)
                .with(authentication(memberAuth))
                .with(csrf()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}/shopping-items", groupId)
                .with(authentication(memberAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].displayName").value("卵"));
  }

  @Test
  void listItemsRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/groups/{groupId}/shopping-items", groupId))
        .andExpect(status().isUnauthorized());
  }

  private UUID createItem(String displayName, int quantity) throws Exception {
    String body =
        objectMapper.writeValueAsString(
            new ShoppingItemCreateRequest(null, displayName, new BigDecimal(quantity), null));
    String json =
        mockMvc
            .perform(
                post("/api/v1/groups/{groupId}/shopping-items", groupId)
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

  private void checkItem(UUID itemId) throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/shopping-items/{id}", itemId)
                .with(authentication(memberAuth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checked\":true}"))
        .andExpect(status().isOk());
  }
}
