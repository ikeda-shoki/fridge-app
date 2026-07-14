package com.example.fridgeapp.fridge;

import com.example.fridgeapp.auth.AuthenticatedUser;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 冷蔵庫アイテムの画像 API（FRG-08）。アップロードと削除を扱う。対象アイテムのグループメンバーのみ操作できる。 */
@RestController
@RequestMapping("/api/v1/fridge-items/{id}/image")
public class FridgeItemImageController {

  private final FridgeItemImageService fridgeItemImageService;

  public FridgeItemImageController(FridgeItemImageService fridgeItemImageService) {
    this.fridgeItemImageService = fridgeItemImageService;
  }

  /** 画像をアップロードする（JPEG / PNG、5MB まで）。既存の画像があれば置き換える。 */
  @PostMapping(consumes = "multipart/form-data")
  public ResponseEntity<FridgeItemImageResponse> uploadImage(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @RequestParam("file") MultipartFile file) {
    String imagePath = fridgeItemImageService.uploadImage(principal.userId(), id, file);
    return ResponseEntity.ok(new FridgeItemImageResponse(imagePath));
  }

  /** 画像を削除する。画像が無い場合も成功として扱う。 */
  @DeleteMapping
  public ResponseEntity<Void> deleteImage(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    fridgeItemImageService.deleteImage(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }
}
