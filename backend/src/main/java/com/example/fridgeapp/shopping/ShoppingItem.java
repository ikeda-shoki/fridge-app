package com.example.fridgeapp.shopping;

import com.example.fridgeapp.common.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 買い物リストアイテム。グループ単位で管理する。
 *
 * <p>冷蔵庫アイテムと異なり消費履歴を参照されないため、削除・チェック済み一括クリアは物理削除でよい。
 */
@Entity
@Table(name = "shopping_items")
public class ShoppingItem extends AbstractAuditableEntity {

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

  @Column(name = "memo")
  private String memo;

  @Column(name = "checked", nullable = false)
  private boolean checked;

  protected ShoppingItem() {}

  public ShoppingItem(
      UUID groupId, UUID foodMasterId, String displayName, BigDecimal quantity, String memo) {
    this.groupId = groupId;
    this.foodMasterId = foodMasterId;
    this.displayName = displayName;
    this.quantity = quantity;
    this.memo = memo;
    this.checked = false;
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

  public String getMemo() {
    return memo;
  }

  public boolean isChecked() {
    return checked;
  }

  /** 編集（SHP-02 のチェック切り替えを含む）。null の項目は変更しない。 */
  public void update(
      UUID foodMasterId, String displayName, BigDecimal quantity, String memo, Boolean checked) {
    if (foodMasterId != null) {
      this.foodMasterId = foodMasterId;
    }
    if (displayName != null) {
      this.displayName = displayName;
    }
    if (quantity != null) {
      this.quantity = quantity;
    }
    if (memo != null) {
      this.memo = memo;
    }
    if (checked != null) {
      this.checked = checked;
    }
  }
}
