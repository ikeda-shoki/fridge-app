package com.example.fridgeapp.fridge;

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
    return FridgeItemCategory.fromLabel(label)
        .orElseThrow(() -> new IllegalStateException("未知のカテゴリが保存されています: " + label));
  }
}
