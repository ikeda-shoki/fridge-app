package com.example.fridgeapp.fridge;

import java.util.Arrays;
import java.util.Optional;

/**
 * 冷蔵庫アイテムのカテゴリ。
 *
 * <p>分類は食材マスタ（{@code food_master.default_category}）を正とし、マスタのどの分類にも当てはまらない食材のために「飲み物」「その他」を加えている。DB
 * には日本語のラベルをそのまま保存するため、マスタ選択時のカテゴリ自動入力（MST-04）はラベルの一致で成立する。
 */
public enum FridgeItemCategory {
  VEGETABLE("野菜"),
  MUSHROOM("きのこ"),
  FRUIT("果物"),
  MEAT("肉類"),
  SEAFOOD("魚介類"),
  DAIRY("乳製品"),
  SOY("豆腐・大豆"),
  SEASONING("調味料"),
  GRAIN("穀類・麺"),
  DRINK("飲み物"),
  OTHER("その他");

  private final String label;

  FridgeItemCategory(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  /** 日本語ラベルから列挙値を引く。未知のラベルの場合は空を返す。 */
  public static Optional<FridgeItemCategory> fromLabel(String label) {
    return Arrays.stream(values()).filter(category -> category.label.equals(label)).findFirst();
  }
}
