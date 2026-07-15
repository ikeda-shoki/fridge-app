package com.example.fridgeapp.shopping;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.foodmaster.FoodMasterRepository;
import com.example.fridgeapp.fridge.FridgeItem;
import com.example.fridgeapp.fridge.FridgeItemCategory;
import com.example.fridgeapp.fridge.FridgeItemException;
import com.example.fridgeapp.fridge.FridgeItemRepository;
import com.example.fridgeapp.fridge.FridgeItemResponse;
import com.example.fridgeapp.group.GroupAccessGuard;
import com.example.fridgeapp.group.GroupException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 買い物リストの CRUD・冷蔵庫への移動を扱う。
 *
 * <p>いずれの操作も、対象グループのメンバーであることを {@link GroupAccessGuard} で検証してから実行する。
 */
@Service
public class ShoppingItemService {

  private final ShoppingItemRepository shoppingItemRepository;
  private final FridgeItemRepository fridgeItemRepository;
  private final FoodMasterRepository foodMasterRepository;
  private final GroupAccessGuard groupAccessGuard;

  public ShoppingItemService(
      ShoppingItemRepository shoppingItemRepository,
      FridgeItemRepository fridgeItemRepository,
      FoodMasterRepository foodMasterRepository,
      GroupAccessGuard groupAccessGuard) {
    this.shoppingItemRepository = shoppingItemRepository;
    this.fridgeItemRepository = fridgeItemRepository;
    this.foodMasterRepository = foodMasterRepository;
    this.groupAccessGuard = groupAccessGuard;
  }

  /**
   * 一覧。登録順に返す。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   */
  @Transactional(readOnly = true)
  public List<ShoppingItemResponse> listItems(UUID userId, UUID groupId) {
    groupAccessGuard.assertMember(groupId, userId);
    return shoppingItemRepository.findByGroupIdOrderByCreatedAtAsc(groupId).stream()
        .map(ShoppingItemResponse::from)
        .toList();
  }

  /**
   * アイテム追加（SHP-01）。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws ShoppingItemException 指定した食材マスタが存在しない場合（{@link AppError#FOOD_MASTER_NOT_FOUND}）
   */
  @Transactional
  public ShoppingItemResponse createItem(
      UUID userId, UUID groupId, ShoppingItemCreateRequest request) {
    groupAccessGuard.assertMember(groupId, userId);
    assertFoodMasterExists(request.foodMasterId());

    ShoppingItem item =
        new ShoppingItem(
            groupId,
            request.foodMasterId(),
            request.displayName(),
            request.quantity(),
            request.memo());
    return ShoppingItemResponse.from(shoppingItemRepository.save(item));
  }

  /**
   * アイテム編集（SHP-02 のチェック切り替えを含む）。null の項目は変更しない。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws ShoppingItemException アイテムが存在しない場合（{@link
   *     AppError#SHOPPING_ITEM_NOT_FOUND}）、指定した食材マスタが存在しない場合（{@link
   *     AppError#FOOD_MASTER_NOT_FOUND}）
   */
  @Transactional
  public ShoppingItemResponse updateItem(
      UUID userId, UUID itemId, ShoppingItemUpdateRequest request) {
    ShoppingItem item = findItem(userId, itemId);
    assertFoodMasterExists(request.foodMasterId());

    item.update(
        request.foodMasterId(),
        request.displayName(),
        request.quantity(),
        request.memo(),
        request.checked());
    return ShoppingItemResponse.from(shoppingItemRepository.save(item));
  }

  /**
   * アイテム削除（SHP-03）。消費履歴を持たないため物理削除する。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws ShoppingItemException アイテムが存在しない場合（{@link AppError#SHOPPING_ITEM_NOT_FOUND}）
   */
  @Transactional
  public void deleteItem(UUID userId, UUID itemId) {
    ShoppingItem item = findItem(userId, itemId);
    shoppingItemRepository.delete(item);
  }

  /**
   * チェック済みアイテムを冷蔵庫アイテムとして登録し、買い物リストから削除する（SHP-04）。数量・食材マスタ参照・メモは引き継ぐ。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws ShoppingItemException アイテムが存在しない場合（{@link
   *     AppError#SHOPPING_ITEM_NOT_FOUND}）、チェックされていない場合（{@link AppError#SHOPPING_ITEM_NOT_CHECKED}）
   * @throws FridgeItemException カテゴリが未知の場合（{@link AppError#INVALID_FRIDGE_ITEM_CATEGORY}）
   */
  @Transactional
  public FridgeItemResponse moveToFridge(UUID userId, UUID itemId, MoveToFridgeRequest request) {
    ShoppingItem item = findItem(userId, itemId);
    if (!item.isChecked()) {
      throw new ShoppingItemException(AppError.SHOPPING_ITEM_NOT_CHECKED);
    }

    FridgeItem fridgeItem =
        new FridgeItem(
            item.getGroupId(),
            item.getFoodMasterId(),
            item.getDisplayName(),
            item.getQuantity(),
            request.unit(),
            parseCategory(request.category()),
            request.expiresAt(),
            request.purchasedAt() == null ? LocalDate.now() : request.purchasedAt(),
            request.purchasedBy() == null ? userId : request.purchasedBy(),
            item.getMemo());
    FridgeItemResponse response = FridgeItemResponse.from(fridgeItemRepository.save(fridgeItem));

    shoppingItemRepository.delete(item);
    return response;
  }

  /**
   * チェック済みアイテムの一括クリア（SHP-05）。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   */
  @Transactional
  public void clearCheckedItems(UUID userId, UUID groupId) {
    groupAccessGuard.assertMember(groupId, userId);
    shoppingItemRepository.deleteCheckedByGroupId(groupId);
  }

  /** 編集・削除・移動の対象となるアイテムを、グループメンバー権限とあわせて解決する。 */
  private ShoppingItem findItem(UUID userId, UUID itemId) {
    ShoppingItem item =
        shoppingItemRepository
            .findById(itemId)
            .orElseThrow(() -> new ShoppingItemException(AppError.SHOPPING_ITEM_NOT_FOUND));
    groupAccessGuard.assertMember(item.getGroupId(), userId);
    return item;
  }

  private void assertFoodMasterExists(UUID foodMasterId) {
    if (foodMasterId != null && !foodMasterRepository.existsById(foodMasterId)) {
      throw new ShoppingItemException(AppError.FOOD_MASTER_NOT_FOUND);
    }
  }

  private FridgeItemCategory parseCategory(String label) {
    return FridgeItemCategory.fromLabel(label)
        .orElseThrow(() -> new FridgeItemException(AppError.INVALID_FRIDGE_ITEM_CATEGORY));
  }
}
