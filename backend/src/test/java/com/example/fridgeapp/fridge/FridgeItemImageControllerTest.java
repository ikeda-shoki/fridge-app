package com.example.fridgeapp.fridge;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fridgeapp.auth.AuthenticatedUser;
import com.example.fridgeapp.support.AbstractIntegrationTest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class FridgeItemImageControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private UUID fridgeItemId;
  private Authentication authenticatedUser;
  private Authentication outsider;

  @BeforeEach
  void seedFridgeItem() {
    UUID groupId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();
    fridgeItemId = UUID.randomUUID();
    jdbcTemplate.update("INSERT INTO groups (id, name) VALUES (?, ?)", groupId, "テストグループ");
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
    jdbcTemplate.update(
        "INSERT INTO fridge_items (id, group_id, display_name, quantity, status) "
            + "VALUES (?, ?, ?, 1, 'ACTIVE')",
        fridgeItemId,
        groupId,
        "テスト食材");
    authenticatedUser =
        new UsernamePasswordAuthenticationToken(new AuthenticatedUser(memberId), null, List.of());
    outsider =
        new UsernamePasswordAuthenticationToken(new AuthenticatedUser(outsiderId), null, List.of());
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
  void uploadImageForOtherGroupsItemIsForbidden() throws Exception {
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", out.toByteArray());

    mockMvc
        .perform(
            multipart("/api/v1/fridge-items/{id}/image", fridgeItemId)
                .file(file)
                .with(authentication(outsider))
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
  }

  @Test
  void getImageReturnsUploadedImage() throws Exception {
    uploadJpeg(fridgeItemId, authenticatedUser);

    mockMvc
        .perform(
            get("/api/v1/fridge-items/{id}/image", fridgeItemId)
                .with(authentication(authenticatedUser)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
  }

  @Test
  void getImageReturnsNotFoundWhenItemHasNoImage() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/fridge-items/{id}/image", fridgeItemId)
                .with(authentication(authenticatedUser)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("FRIDGE_ITEM_IMAGE_NOT_FOUND"));
  }

  @Test
  void getImageForOtherGroupsItemIsForbidden() throws Exception {
    uploadJpeg(fridgeItemId, authenticatedUser);

    mockMvc
        .perform(
            get("/api/v1/fridge-items/{id}/image", fridgeItemId).with(authentication(outsider)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
  }

  @Test
  void getImageWithoutAuthenticationIsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/fridge-items/{id}/image", fridgeItemId))
        .andExpect(status().isUnauthorized());
  }

  private void uploadJpeg(UUID itemId, Authentication user) throws Exception {
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", out.toByteArray());

    mockMvc
        .perform(
            multipart("/api/v1/fridge-items/{id}/image", itemId)
                .file(file)
                .with(authentication(user))
                .with(csrf()))
        .andExpect(status().isOk());
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
