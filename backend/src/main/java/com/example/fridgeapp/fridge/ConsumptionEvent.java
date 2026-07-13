package com.example.fridgeapp.fridge;

import com.example.fridgeapp.common.AbstractCreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 消費履歴。将来のレシピ連携で消費を集計するため、MVP から記録する（履歴は不変・更新削除不可）。 */
@Entity
@Table(name = "consumption_events")
public class ConsumptionEvent extends AbstractCreatedOnlyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "fridge_item_id", nullable = false, updatable = false)
  private UUID fridgeItemId;

  @Column(name = "quantity_consumed", nullable = false, updatable = false)
  private BigDecimal quantityConsumed;

  @Enumerated(EnumType.STRING)
  @Column(name = "reason", nullable = false, updatable = false)
  private ConsumptionReason reason;

  @Column(name = "recipe_ref", updatable = false)
  private String recipeRef;

  @Column(name = "consumed_at", nullable = false, updatable = false)
  private Instant consumedAt;

  protected ConsumptionEvent() {}

  public ConsumptionEvent(
      UUID fridgeItemId,
      BigDecimal quantityConsumed,
      ConsumptionReason reason,
      String recipeRef,
      Instant consumedAt) {
    this.fridgeItemId = fridgeItemId;
    this.quantityConsumed = quantityConsumed;
    this.reason = reason;
    this.recipeRef = recipeRef;
    this.consumedAt = consumedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getFridgeItemId() {
    return fridgeItemId;
  }

  public BigDecimal getQuantityConsumed() {
    return quantityConsumed;
  }

  public ConsumptionReason getReason() {
    return reason;
  }

  public String getRecipeRef() {
    return recipeRef;
  }

  public Instant getConsumedAt() {
    return consumedAt;
  }
}
