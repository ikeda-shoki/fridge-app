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

/**
 * 冷蔵庫アイテムの画像アップロード・削除を扱う（FRG-08）。
 *
 * <p>受け付けるのは JPEG / PNG のみで、判定は拡張子や Content-Type ではなくマジックバイトで行う（{@link ImageFormat}）。保存前に長辺 800px
 * へ縮小する。操作には対象アイテムのグループメンバーであることが必要。
 */
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

  /**
   * 画像をアップロードし、保存先パスを返す。既存の画像があれば差し替え、古い画像は削除する。
   *
   * @throws GroupException 操作ユーザーがアイテムのグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws FridgeItemException アイテムが存在しない場合、JPEG / PNG 以外・空ファイル（{@link
   *     AppError#INVALID_IMAGE_FORMAT}）、5MB 超（{@link AppError#IMAGE_TOO_LARGE}）、縮小に失敗した場合（{@link
   *     AppError#IMAGE_PROCESSING_FAILED}）
   */
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

  /**
   * 画像を削除する。画像が登録されていない場合は何もしない。
   *
   * @throws GroupException 操作ユーザーがアイテムのグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws FridgeItemException アイテムが存在しない場合（{@link AppError#FRIDGE_ITEM_NOT_FOUND}）
   */
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
