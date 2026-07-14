package com.example.fridgeapp.fridge;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.group.GroupAccessGuard;
import com.example.fridgeapp.storage.StorageService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FridgeItemImageService {

  private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
  private static final int MAX_DIMENSION = 800;

  private final FridgeItemRepository fridgeItemRepository;
  private final GroupAccessGuard groupAccessGuard;
  private final StorageService storageService;

  public FridgeItemImageService(
      FridgeItemRepository fridgeItemRepository,
      GroupAccessGuard groupAccessGuard,
      StorageService storageService) {
    this.fridgeItemRepository = fridgeItemRepository;
    this.groupAccessGuard = groupAccessGuard;
    this.storageService = storageService;
  }

  @Transactional
  public String uploadImage(UUID userId, UUID fridgeItemId, MultipartFile file) {
    FridgeItem fridgeItem = findFridgeItem(userId, fridgeItemId);

    if (file.isEmpty()) {
      throw new FridgeItemException(AppError.INVALID_IMAGE_FORMAT);
    }
    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new FridgeItemException(AppError.IMAGE_TOO_LARGE);
    }
    ImageFormat format =
        ImageFormat.detect(file)
            .orElseThrow(() -> new FridgeItemException(AppError.INVALID_IMAGE_FORMAT));

    byte[] resized = resize(file, format);

    String oldImagePath = fridgeItem.getImagePath();
    String newImagePath = storageService.store(resized, format.extension());
    fridgeItem.replaceImage(newImagePath);
    fridgeItemRepository.save(fridgeItem);

    if (oldImagePath != null) {
      storageService.delete(oldImagePath);
    }
    return newImagePath;
  }

  @Transactional
  public void deleteImage(UUID userId, UUID fridgeItemId) {
    FridgeItem fridgeItem = findFridgeItem(userId, fridgeItemId);
    String imagePath = fridgeItem.getImagePath();
    if (imagePath == null) {
      return;
    }
    fridgeItem.clearImage();
    fridgeItemRepository.save(fridgeItem);
    storageService.delete(imagePath);
  }

  private FridgeItem findFridgeItem(UUID userId, UUID id) {
    FridgeItem fridgeItem =
        fridgeItemRepository
            .findById(id)
            .orElseThrow(() -> new FridgeItemException(AppError.FRIDGE_ITEM_NOT_FOUND));
    groupAccessGuard.assertMember(fridgeItem.getGroupId(), userId);
    return fridgeItem;
  }

  private byte[] resize(MultipartFile file, ImageFormat format) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      Thumbnails.of(file.getInputStream())
          .size(MAX_DIMENSION, MAX_DIMENSION)
          .outputFormat(format.extension())
          .toOutputStream(output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new FridgeItemException(AppError.IMAGE_PROCESSING_FAILED);
    }
  }
}
