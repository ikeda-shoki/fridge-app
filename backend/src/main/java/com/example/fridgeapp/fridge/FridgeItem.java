package com.example.fridgeapp.fridge;

import com.example.fridgeapp.common.AbstractAuditableEntity;
import com.example.fridgeapp.common.AppError;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 冷蔵庫アイテム。グループ単位で管理する。
 *
 * <p>消費履歴（{@link ConsumptionEvent}）から参照されるため物理削除はせず、消費しきった場合・削除した場合は {@link FridgeItemStatus}
 * で状態を表す。カテゴリは日本語ラベルとして保存する（{@link FridgeItemCategoryConverter}）。
 */
@Entity
@Table(name = "fridge_items")
public class FridgeItem extends AbstractAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Column(name = "food_master_id")
  private UUID foodMasterId;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "unit", length = 20)
  private String unit;

  @Column(name = "category", length = 50)
  private FridgeItemCategory category;

  @Column(name = "expires_at")
  private LocalDate expiresAt;

  @Column(name = "purchased_at")
  private LocalDate purchasedAt;

  @Column(name = "purchased_by")
  private UUID purchasedBy;

  @Column(name = "memo")
  private String memo;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FridgeItemStatus status;

  @Column(name = "image_path")
  private String imagePath;

  protected FridgeItem() {}

  public FridgeItem(
      UUID groupId,
      UUID foodMasterId,
      String displayName,
      BigDecimal quantity,
      String unit,
      FridgeItemCategory category,
      LocalDate expiresAt,
      LocalDate purchasedAt,
      UUID purchasedBy,
      String memo) {
    this.groupId = groupId;
    this.foodMasterId = foodMasterId;
    this.displayName = displayName;
    this.quantity = quantity;
    this.unit = unit;
    this.category = category;
    this.expiresAt = expiresAt;
    this.purchasedAt = purchasedAt;
    this.purchasedBy = purchasedBy;
    this.memo = memo;
    this.status = FridgeItemStatus.ACTIVE;
  }

  public UUID getId() {
    return id;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public UUID getFoodMasterId() {
    return foodMasterId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public String getUnit() {
    return unit;
  }

  public FridgeItemCategory getCategory() {
    return category;
  }

  public LocalDate getExpiresAt() {
    return expiresAt;
  }

  public LocalDate getPurchasedAt() {
    return purchasedAt;
  }

  public UUID getPurchasedBy() {
    return purchasedBy;
  }

  public String getMemo() {
    return memo;
  }

  public FridgeItemStatus getStatus() {
    return status;
  }

  public String getImagePath() {
    return imagePath;
  }

  /** 編集・消費の対象となる状態（消費済み・削除済みでない）なら true。 */
  public boolean isActive() {
    return status == FridgeItemStatus.ACTIVE;
  }

  /** 編集（FRG-02）。null の項目は変更しない。 */
  public void update(
      UUID foodMasterId,
      String displayName,
      BigDecimal quantity,
      String unit,
      FridgeItemCategory category,
      LocalDate expiresAt,
      LocalDate purchasedAt,
      UUID purchasedBy,
      String memo) {
    if (foodMasterId != null) {
      this.foodMasterId = foodMasterId;
    }
    if (displayName != null) {
      this.displayName = displayName;
    }
    if (quantity != null) {
      this.quantity = quantity;
    }
    if (unit != null) {
      this.unit = unit;
    }
    if (category != null) {
      this.category = category;
    }
    if (expiresAt != null) {
      this.expiresAt = expiresAt;
    }
    if (purchasedAt != null) {
      this.purchasedAt = purchasedAt;
    }
    if (purchasedBy != null) {
      this.purchasedBy = purchasedBy;
    }
    if (memo != null) {
      this.memo = memo;
    }
  }

  /**
   * 数量を消費する（FRG-07）。残数が 0 になった場合は消費済みへ遷移する。
   *
   * @throws FridgeItemException 消費量が残数を超える場合（{@link AppError#INSUFFICIENT_QUANTITY}）
   */
  public void consume(BigDecimal quantityConsumed) {
    if (quantityConsumed.compareTo(this.quantity) > 0) {
      throw new FridgeItemException(AppError.INSUFFICIENT_QUANTITY);
    }
    this.quantity = this.quantity.subtract(quantityConsumed);
    if (this.quantity.compareTo(BigDecimal.ZERO) == 0) {
      this.status = FridgeItemStatus.CONSUMED;
    }
  }

  /** 論理削除（FRG-03）。消費履歴を残すため物理削除しない。 */
  public void markDeleted() {
    this.status = FridgeItemStatus.DELETED;
  }

  /** 画像を差し替える。古い画像の実体削除は呼び出し側（{@link FridgeItemImageService}）が行う。 */
  public void replaceImage(String newImagePath) {
    this.imagePath = newImagePath;
  }

  /** 画像の参照を外す。実体削除は呼び出し側が行う。 */
  public void clearImage() {
    this.imagePath = null;
  }
}
