package com.example.fridgeapp.fridge;

import com.example.fridgeapp.auth.AuthenticatedUser;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 冷蔵庫アイテムの画像 API（FRG-08）。取得・アップロード・削除を扱う。対象アイテムのグループメンバーのみ操作できる。 */
@RestController
@RequestMapping("/api/v1/fridge-items/{id}/image")
public class FridgeItemImageController {

  private static final Duration IMAGE_CACHE_DURATION = Duration.ofDays(30);

  private final FridgeItemImageService fridgeItemImageService;

  public FridgeItemImageController(FridgeItemImageService fridgeItemImageService) {
    this.fridgeItemImageService = fridgeItemImageService;
  }

  /**
   * 画像を取得する（一覧のサムネイル・詳細表示用）。
   *
   * <p>画像の実体は差し替えのたびに別パスへ保存されるため、クライアントは一覧で得た {@code imagePath} をクエリパラメータ（例: {@code
   * ?v=xxx.jpg}）に付けて URL を変化させることでキャッシュを破棄できる。サーバー側はこのパラメータを読まない。この前提で長めのキャッシュを許可する。
   *
   * <p>認可を通す必要があるため {@code private} キャッシュとし、共有キャッシュには保存させない。
   */
  @GetMapping
  public ResponseEntity<byte[]> getImage(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    FridgeItemImageContent image = fridgeItemImageService.loadImage(principal.userId(), id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .cacheControl(CacheControl.maxAge(IMAGE_CACHE_DURATION).cachePrivate())
        .body(image.content());
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
