package com.example.fridgeapp.fridge;

import com.example.fridgeapp.common.AppError;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** {@link FridgeItemCategory} を日本語ラベルとして DB に保存する（食材マスタの分類と同じ値で保持するため）。 */
@Converter(autoApply = true)
public class FridgeItemCategoryConverter implements AttributeConverter<FridgeItemCategory, String> {

  @Override
  public String convertToDatabaseColumn(FridgeItemCategory category) {
    return category == null ? null : category.label();
  }

  @Override
  public FridgeItemCategory convertToEntityAttribute(String label) {
    if (label == null) {
      return null;
    }
    // 業務エラーではなく DB のデータ不整合。GlobalExceptionHandler で 500（固定メッセージ）になり、詳細はログにのみ残す
    return FridgeItemCategory.fromLabel(label)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    AppError.CORRUPTED_FRIDGE_ITEM_CATEGORY.getMessage() + ": " + label));
  }
}
