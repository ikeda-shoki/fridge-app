package com.example.fridgeapp.fridge;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fridgeapp.auth.AuthenticatedUser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FridgeItemImageControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private UUID fridgeItemId;
  private Authentication authenticatedUser;

  @BeforeEach
  void seedFridgeItem() {
    UUID groupId = UUID.randomUUID();
    fridgeItemId = UUID.randomUUID();
    jdbcTemplate.update("INSERT INTO groups (id, name) VALUES (?, ?)", groupId, "テストグループ");
    jdbcTemplate.update(
        "INSERT INTO fridge_items (id, group_id, display_name, quantity, status) "
            + "VALUES (?, ?, ?, 1, 'ACTIVE')",
        fridgeItemId,
        groupId,
        "テスト食材");
    authenticatedUser =
        new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(UUID.randomUUID()), null, List.of());
  }

  @Test
  void uploadImageResizesAndReturnsImagePath() throws Exception {
    BufferedImage image = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", out.toByteArray());

    mockMvc
        .perform(
            multipart("/api/v1/fridge-items/{id}/image", fridgeItemId)
                .file(file)
                .with(authentication(authenticatedUser))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imagePath").isNotEmpty());
  }

  @Test
  void uploadImageRejectsTextFile() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "note.txt", "text/plain", "not an image".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/fridge-items/{id}/image", fridgeItemId)
                .file(file)
                .with(authentication(authenticatedUser))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_IMAGE_FORMAT"));
  }

  @Test
  void uploadImageForUnknownFridgeItemReturnsNotFound() throws Exception {
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", out.toByteArray());

    mockMvc
        .perform(
            multipart("/api/v1/fridge-items/{id}/image", UUID.randomUUID())
                .file(file)
                .with(authentication(authenticatedUser))
                .with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("FRIDGE_ITEM_NOT_FOUND"));
  }

  @Test
  void uploadWithoutAuthenticationIsUnauthorized() throws Exception {
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", out.toByteArray());

    mockMvc
        .perform(multipart("/api/v1/fridge-items/{id}/image", fridgeItemId).file(file).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteImageIsNoContentWhenNoImageStored() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/fridge-items/{id}/image", fridgeItemId)
                .with(authentication(authenticatedUser))
                .with(csrf()))
        .andExpect(status().isNoContent());
  }
}
