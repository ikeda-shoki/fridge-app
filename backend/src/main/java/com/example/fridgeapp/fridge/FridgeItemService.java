package com.example.fridgeapp.fridge;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.common.LikeEscaper;
import com.example.fridgeapp.foodmaster.FoodMasterRepository;
import com.example.fridgeapp.group.GroupAccessGuard;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 冷蔵庫アイテムの CRUD・検索・消費を扱う。
 *
 * <p>いずれの操作も、対象グループのメンバーであることを {@link GroupAccessGuard} で検証してから実行する。
 */
@Service
public class FridgeItemService {

  private final FridgeItemRepository fridgeItemRepository;
  private final ConsumptionEventRepository consumptionEventRepository;
  private final FoodMasterRepository foodMasterRepository;
  private final GroupAccessGuard groupAccessGuard;

  public FridgeItemService(
      FridgeItemRepository fridgeItemRepository,
      ConsumptionEventRepository consumptionEventRepository,
      FoodMasterRepository foodMasterRepository,
      GroupAccessGuard groupAccessGuard) {
    this.fridgeItemRepository = fridgeItemRepository;
    this.consumptionEventRepository = consumptionEventRepository;
    this.foodMasterRepository = foodMasterRepository;
    this.groupAccessGuard = groupAccessGuard;
  }

  /**
   * 一覧（FRG-04・FRG-06・FRG-09）。賞味期限が近い順、カテゴリ絞り込みと名前検索は併用できる。
   *
   * @param category 日本語のカテゴリラベル。null なら絞り込まない
   * @param query 名前の部分一致検索語。null・空なら絞り込まない
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws FridgeItemException 未知のカテゴリを指定した場合（{@link AppError#INVALID_FRIDGE_ITEM_CATEGORY}）
   */
  @Transactional(readOnly = true)
  public List<FridgeItemResponse> listItems(
      UUID userId, UUID groupId, String category, String query) {
    groupAccessGuard.assertMember(groupId, userId);

    FridgeItemCategory categoryFilter = category == null ? null : parseCategory(category);
    String keyword = (query == null || query.isBlank()) ? "" : LikeEscaper.escape(query.trim());

    return fridgeItemRepository
        .searchActiveByGroupId(groupId, categoryFilter == null, categoryFilter, keyword)
        .stream()
        .map(FridgeItemResponse::from)
        .toList();
  }

  /**
   * アイテム登録（FRG-01）。購入日・購入者は未指定なら登録日・登録ユーザーで補完する。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws FridgeItemException カテゴリが未知（{@link
   *     AppError#INVALID_FRIDGE_ITEM_CATEGORY}）、指定した食材マスタが存在しない場合（{@link
   *     AppError#FOOD_MASTER_NOT_FOUND}）
   */
  @Transactional
  public FridgeItemResponse createItem(UUID userId, UUID groupId, FridgeItemCreateRequest request) {
    groupAccessGuard.assertMember(groupId, userId);
    assertFoodMasterExists(request.foodMasterId());

    FridgeItem item =
        new FridgeItem(
            groupId,
            request.foodMasterId(),
            request.displayName(),
            request.quantity(),
            request.unit(),
            parseCategory(request.category()),
            request.expiresAt(),
            request.purchasedAt() == null ? LocalDate.now() : request.purchasedAt(),
            request.purchasedBy() == null ? userId : request.purchasedBy(),
            request.memo());
    return FridgeItemResponse.from(fridgeItemRepository.save(item));
  }

  /**
   * アイテム編集（FRG-02）。数量変更もここで行う。null の項目は変更しない。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws FridgeItemException アイテムが存在しない（{@link
   *     AppError#FRIDGE_ITEM_NOT_FOUND}）、消費済み・削除済みの場合（{@link AppError#FRIDGE_ITEM_NOT_ACTIVE}）
   */
  @Transactional
  public FridgeItemResponse updateItem(UUID userId, UUID itemId, FridgeItemUpdateRequest request) {
    FridgeItem item = findActiveItem(userId, itemId);
    assertFoodMasterExists(request.foodMasterId());

    item.update(
        request.foodMasterId(),
        request.displayName(),
        request.quantity(),
        request.unit(),
        request.category() == null ? null : parseCategory(request.category()),
        request.expiresAt(),
        request.purchasedAt(),
        request.purchasedBy(),
        request.memo());
    return FridgeItemResponse.from(fridgeItemRepository.save(item));
  }

  /**
   * アイテム削除（FRG-03）。消費履歴を残すため論理削除する。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws FridgeItemException アイテムが存在しない、既に消費済み・削除済みの場合
   */
  @Transactional
  public void deleteItem(UUID userId, UUID itemId) {
    FridgeItem item = findActiveItem(userId, itemId);
    item.markDeleted();
    fridgeItemRepository.save(item);
  }

  /**
   * 数量消費（FRG-07）。残数が 0 になったら消費済みへ遷移し、履歴を consumption_events に記録する。
   *
   * @throws GroupException 操作ユーザーがグループのメンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）
   * @throws FridgeItemException アイテムが存在しない・非アクティブの場合、消費量が残数を超える場合（{@link
   *     AppError#INSUFFICIENT_QUANTITY}）
   */
  @Transactional
  public FridgeItemResponse consumeItem(
      UUID userId, UUID itemId, FridgeItemConsumeRequest request) {
    FridgeItem item = findActiveItem(userId, itemId);

    BigDecimal quantityConsumed = request.quantity();
    item.consume(quantityConsumed);
    fridgeItemRepository.save(item);

    ConsumptionReason reason =
        request.reason() == null ? ConsumptionReason.MANUAL : request.reason();
    consumptionEventRepository.save(
        new ConsumptionEvent(item.getId(), quantityConsumed, reason, null, Instant.now()));

    return FridgeItemResponse.from(item);
  }

  /** 編集・削除・消費の対象となるアイテムを、グループメンバー権限とあわせて解決する。 */
  private FridgeItem findActiveItem(UUID userId, UUID itemId) {
    FridgeItem item =
        fridgeItemRepository
            .findById(itemId)
            .orElseThrow(() -> new FridgeItemException(AppError.FRIDGE_ITEM_NOT_FOUND));
    groupAccessGuard.assertMember(item.getGroupId(), userId);
    if (!item.isActive()) {
      throw new FridgeItemException(AppError.FRIDGE_ITEM_NOT_ACTIVE);
    }
    return item;
  }

  private void assertFoodMasterExists(UUID foodMasterId) {
    if (foodMasterId != null && !foodMasterRepository.existsById(foodMasterId)) {
      throw new FridgeItemException(AppError.FOOD_MASTER_NOT_FOUND);
    }
  }

  private FridgeItemCategory parseCategory(String label) {
    return FridgeItemCategory.fromLabel(label)
        .orElseThrow(() -> new FridgeItemException(AppError.INVALID_FRIDGE_ITEM_CATEGORY));
  }
}
