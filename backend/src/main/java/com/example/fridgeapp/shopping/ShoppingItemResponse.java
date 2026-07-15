package com.example.fridgeapp.shopping;

import java.math.BigDecimal;
import java.util.UUID;

/** 買い物リストアイテム。 */
public record ShoppingItemResponse(
    UUID id,
    UUID groupId,
    UUID foodMasterId,
    String displayName,
    BigDecimal quantity,
    String memo,
    boolean checked) {

  /** エンティティからレスポンスへ変換する。 */
  public static ShoppingItemResponse from(ShoppingItem item) {
    return new ShoppingItemResponse(
        item.getId(),
        item.getGroupId(),
        item.getFoodMasterId(),
        item.getDisplayName(),
        item.getQuantity(),
        item.getMemo(),
        item.isChecked());
  }
}
