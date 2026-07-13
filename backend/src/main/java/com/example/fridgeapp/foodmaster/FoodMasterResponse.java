package com.example.fridgeapp.foodmaster;

import java.util.UUID;

public record FoodMasterResponse(UUID id, String name, String defaultCategory, String defaultUnit) {

  public static FoodMasterResponse from(FoodMaster foodMaster) {
    return new FoodMasterResponse(
        foodMaster.getId(),
        foodMaster.getName(),
        foodMaster.getDefaultCategory(),
        foodMaster.getDefaultUnit());
  }
}
