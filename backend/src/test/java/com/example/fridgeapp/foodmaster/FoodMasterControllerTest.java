package com.example.fridgeapp.foodmaster;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fridgeapp.auth.AuthenticatedUser;
import com.example.fridgeapp.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class FoodMasterControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private final Authentication auth =
      new UsernamePasswordAuthenticationToken(
          new AuthenticatedUser(UUID.randomUUID()), null, List.of());

  @Test
  void searchReturnsSeededMatchesOrderedByReading() throws Exception {
    mockMvc
        .perform(get("/api/v1/food-master/search").param("q", "たま").with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == '玉ねぎ')]").exists())
        .andExpect(jsonPath("$[?(@.name == '卵')]").exists());
  }

  @Test
  void searchReturnsCategoryAndUnitFromMaster() throws Exception {
    mockMvc
        .perform(get("/api/v1/food-master/search").param("q", "にんじん").with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("人参"))
        .andExpect(jsonPath("$[0].defaultCategory").value("野菜"))
        .andExpect(jsonPath("$[0].defaultUnit").value("本"));
  }

  @Test
  void searchReturnsEmptyListWhenQueryBlank() throws Exception {
    mockMvc
        .perform(get("/api/v1/food-master/search").with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void searchReturnsEmptyListWhenNoMatch() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/food-master/search").param("q", "存在しない食材のよみがな").with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void searchRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/food-master/search").param("q", "たま"))
        .andExpect(status().isUnauthorized());
  }
}
