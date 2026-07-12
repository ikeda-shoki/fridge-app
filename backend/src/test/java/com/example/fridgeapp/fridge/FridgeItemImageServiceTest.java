package com.example.fridgeapp.fridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fridgeapp.storage.StorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FridgeItemImageServiceTest {

  @Mock private FridgeItemRepository fridgeItemRepository;
  @Mock private StorageService storageService;

  private FridgeItemImageService imageService;

  @BeforeEach
  void setUp() {
    imageService = new FridgeItemImageService(fridgeItemRepository, storageService);
  }

  @Test
  void uploadImageResizesStoresAndUpdatesFridgeItem() throws Exception {
    UUID id = UUID.randomUUID();
    FridgeItem fridgeItem = fridgeItemWithId(id);
    when(fridgeItemRepository.findById(id)).thenReturn(Optional.of(fridgeItem));
    when(storageService.store(any(byte[].class), eq("jpg"))).thenReturn("new-image.jpg");

    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes(1200, 900));

    String result = imageService.uploadImage(id, file);

    assertThat(result).isEqualTo("new-image.jpg");
    assertThat(fridgeItem.getImagePath()).isEqualTo("new-image.jpg");
    verify(fridgeItemRepository).save(fridgeItem);
    verify(storageService, never()).delete(anyString());
  }

  @Test
  void uploadImageDeletesPreviousImageWhenReplacing() throws Exception {
    UUID id = UUID.randomUUID();
    FridgeItem fridgeItem = fridgeItemWithId(id);
    fridgeItem.replaceImage("old-image.png");
    when(fridgeItemRepository.findById(id)).thenReturn(Optional.of(fridgeItem));
    when(storageService.store(any(byte[].class), eq("png"))).thenReturn("new-image.png");

    MockMultipartFile file =
        new MockMultipartFile("file", "photo.png", "image/png", pngBytes(400, 400));

    imageService.uploadImage(id, file);

    verify(storageService).delete("old-image.png");
  }

  @Test
  void uploadImageRejectsFileLargerThan5MB() {
    UUID id = UUID.randomUUID();
    when(fridgeItemRepository.findById(id)).thenReturn(Optional.of(fridgeItemWithId(id)));

    MockMultipartFile file =
        new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

    assertThatThrownBy(() -> imageService.uploadImage(id, file))
        .isInstanceOf(FridgeItemException.class)
        .extracting("error")
        .isEqualTo(com.example.fridgeapp.common.AppError.IMAGE_TOO_LARGE);
  }

  @Test
  void uploadImageRejectsUnsupportedFormat() {
    UUID id = UUID.randomUUID();
    when(fridgeItemRepository.findById(id)).thenReturn(Optional.of(fridgeItemWithId(id)));

    MockMultipartFile file =
        new MockMultipartFile("file", "note.txt", "text/plain", "not an image".getBytes());

    assertThatThrownBy(() -> imageService.uploadImage(id, file))
        .isInstanceOf(FridgeItemException.class)
        .extracting("error")
        .isEqualTo(com.example.fridgeapp.common.AppError.INVALID_IMAGE_FORMAT);
  }

  @Test
  void uploadImageThrowsWhenFridgeItemNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    when(fridgeItemRepository.findById(id)).thenReturn(Optional.empty());

    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes(100, 100));

    assertThatThrownBy(() -> imageService.uploadImage(id, file))
        .isInstanceOf(FridgeItemException.class)
        .extracting("error")
        .isEqualTo(com.example.fridgeapp.common.AppError.FRIDGE_ITEM_NOT_FOUND);
  }

  @Test
  void deleteImageRemovesStoredFileAndClearsPath() {
    UUID id = UUID.randomUUID();
    FridgeItem fridgeItem = fridgeItemWithId(id);
    fridgeItem.replaceImage("existing.jpg");
    when(fridgeItemRepository.findById(id)).thenReturn(Optional.of(fridgeItem));

    imageService.deleteImage(id);

    assertThat(fridgeItem.getImagePath()).isNull();
    verify(storageService).delete("existing.jpg");
    verify(fridgeItemRepository).save(fridgeItem);
  }

  @Test
  void deleteImageIsNoopWhenNoImagePresent() {
    UUID id = UUID.randomUUID();
    FridgeItem fridgeItem = fridgeItemWithId(id);
    when(fridgeItemRepository.findById(id)).thenReturn(Optional.of(fridgeItem));

    imageService.deleteImage(id);

    verify(storageService, never()).delete(anyString());
    verify(fridgeItemRepository, never()).save(any());
  }

  private FridgeItem fridgeItemWithId(UUID id) {
    FridgeItem fridgeItem = new FridgeItem();
    ReflectionTestUtils.setField(fridgeItem, "id", id);
    return fridgeItem;
  }

  private byte[] jpegBytes(int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    return out.toByteArray();
  }

  private byte[] pngBytes(int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "png", out);
    return out.toByteArray();
  }
}
