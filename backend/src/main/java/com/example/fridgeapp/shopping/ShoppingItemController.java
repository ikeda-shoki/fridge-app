package com.example.fridgeapp.shopping;

import com.example.fridgeapp.auth.AuthenticatedUser;
import com.example.fridgeapp.fridge.FridgeItemResponse;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 買い物リスト API（SHP-01〜05）。一覧・追加・編集・削除・冷蔵庫への移動・一括クリアを扱う。
 *
 * <p>いずれも操作ユーザーが対象グループのメンバーである必要がある（非メンバーは 403）。
 */
@RestController
@RequestMapping("/api/v1")
public class ShoppingItemController {

  private final ShoppingItemService shoppingItemService;

  public ShoppingItemController(ShoppingItemService shoppingItemService) {
    this.shoppingItemService = shoppingItemService;
  }

  /** 買い物リストの一覧を返す（登録順）。 */
  @GetMapping("/groups/{groupId}/shopping-items")
  public ResponseEntity<List<ShoppingItemResponse>> listItems(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID groupId) {
    return ResponseEntity.ok(shoppingItemService.listItems(principal.userId(), groupId));
  }

  /** アイテムを追加する（SHP-01）。 */
  @PostMapping("/groups/{groupId}/shopping-items")
  public ResponseEntity<ShoppingItemResponse> createItem(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID groupId,
      @Valid @RequestBody ShoppingItemCreateRequest request) {
    return ResponseEntity.ok(shoppingItemService.createItem(principal.userId(), groupId, request));
  }

  /** アイテムを編集する（SHP-02 のチェック切り替えを含む）。null の項目は変更しない部分更新。 */
  @PatchMapping("/shopping-items/{id}")
  public ResponseEntity<ShoppingItemResponse> updateItem(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @Valid @RequestBody ShoppingItemUpdateRequest request) {
    return ResponseEntity.ok(shoppingItemService.updateItem(principal.userId(), id, request));
  }

  /** アイテムを削除する（SHP-03）。消費履歴を持たないため物理削除する。 */
  @DeleteMapping("/shopping-items/{id}")
  public ResponseEntity<Void> deleteItem(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    shoppingItemService.deleteItem(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }

  /** チェック済みアイテムを冷蔵庫アイテムとして登録する（SHP-04）。登録後、買い物リストから削除される。 */
  @PostMapping("/shopping-items/{id}/move-to-fridge")
  public ResponseEntity<FridgeItemResponse> moveToFridge(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @Valid @RequestBody MoveToFridgeRequest request) {
    return ResponseEntity.ok(shoppingItemService.moveToFridge(principal.userId(), id, request));
  }

  /** チェック済みアイテムを一括で削除する（SHP-05）。 */
  @DeleteMapping("/groups/{groupId}/shopping-items/checked")
  public ResponseEntity<Void> clearCheckedItems(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID groupId) {
    shoppingItemService.clearCheckedItems(principal.userId(), groupId);
    return ResponseEntity.noContent().build();
  }
}
