package com.example.fridgeapp.foodmaster;

import java.util.UUID;

/** 食材マスタのレスポンス。{@code defaultCategory} / {@code defaultUnit} はアイテム登録時の初期値として使う。 */
public record FoodMasterResponse(UUID id, String name, String defaultCategory, String defaultUnit) {

  /** エンティティからレスポンスへ変換する。 */
  public static FoodMasterResponse from(FoodMaster foodMaster) {
    return new FoodMasterResponse(
        foodMaster.getId(),
        foodMaster.getName(),
        foodMaster.getDefaultCategory(),
        foodMaster.getDefaultUnit());
  }
}
