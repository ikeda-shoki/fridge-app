package com.example.fridgeapp.fridge;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 冷蔵庫アイテム。
 *
 * <p>期限ハイライト（FRG-05）の判定はクライアント側で {@code expiresAt} から行う。
 */
public record FridgeItemResponse(
    UUID id,
    UUID groupId,
    UUID foodMasterId,
    String displayName,
    BigDecimal quantity,
    String unit,
    String category,
    LocalDate expiresAt,
    LocalDate purchasedAt,
    UUID purchasedBy,
    String imagePath,
    String memo,
    FridgeItemStatus status) {

  public static FridgeItemResponse from(FridgeItem item) {
    return new FridgeItemResponse(
        item.getId(),
        item.getGroupId(),
        item.getFoodMasterId(),
        item.getDisplayName(),
        item.getQuantity(),
        item.getUnit(),
        item.getCategory() == null ? null : item.getCategory().label(),
        item.getExpiresAt(),
        item.getPurchasedAt(),
        item.getPurchasedBy(),
        item.getImagePath(),
        item.getMemo(),
        item.getStatus());
  }
}
