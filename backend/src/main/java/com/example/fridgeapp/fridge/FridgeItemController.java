package com.example.fridgeapp.fridge;

import com.example.fridgeapp.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 冷蔵庫アイテム API（FRG-01〜04・06・07・09）。一覧・登録・編集・削除・消費を扱う。
 *
 * <p>いずれも操作ユーザーが対象グループのメンバーである必要がある（非メンバーは 403）。
 */
@RestController
@RequestMapping("/api/v1")
public class FridgeItemController {

  private final FridgeItemService fridgeItemService;

  public FridgeItemController(FridgeItemService fridgeItemService) {
    this.fridgeItemService = fridgeItemService;
  }

  /**
   * アイテム一覧を返す。賞味期限が近い順（期限なしは末尾）。
   *
   * <p>{@code category}（カテゴリ絞り込み）と {@code q}（名前の部分一致検索）は併用できる。期限ハイライト（FRG-05）の判定はフロント側で行うため、API は
   * {@code expiresAt} を返すのみ。
   */
  @GetMapping("/groups/{groupId}/fridge-items")
  public ResponseEntity<List<FridgeItemResponse>> listItems(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID groupId,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "q", required = false) String query) {
    return ResponseEntity.ok(
        fridgeItemService.listItems(principal.userId(), groupId, category, query));
  }

  /** アイテムを登録する。購入日・購入者は未指定なら登録日・登録ユーザーで補完される。 */
  @PostMapping("/groups/{groupId}/fridge-items")
  public ResponseEntity<FridgeItemResponse> createItem(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID groupId,
      @Valid @RequestBody FridgeItemCreateRequest request) {
    return ResponseEntity.ok(fridgeItemService.createItem(principal.userId(), groupId, request));
  }

  /** アイテムを編集する（数量変更を含む）。null の項目は変更しない部分更新。 */
  @PatchMapping("/fridge-items/{id}")
  public ResponseEntity<FridgeItemResponse> updateItem(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @Valid @RequestBody FridgeItemUpdateRequest request) {
    return ResponseEntity.ok(fridgeItemService.updateItem(principal.userId(), id, request));
  }

  /** アイテムを削除する。消費履歴を残すため論理削除とする。 */
  @DeleteMapping("/fridge-items/{id}")
  public ResponseEntity<Void> deleteItem(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    fridgeItemService.deleteItem(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }

  /** アイテムを消費する。消費履歴が記録され、残数が 0 になったアイテムは消費済みへ遷移する。 */
  @PostMapping("/fridge-items/{id}/consume")
  public ResponseEntity<FridgeItemResponse> consumeItem(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @Valid @RequestBody FridgeItemConsumeRequest request) {
    return ResponseEntity.ok(fridgeItemService.consumeItem(principal.userId(), id, request));
  }
}
