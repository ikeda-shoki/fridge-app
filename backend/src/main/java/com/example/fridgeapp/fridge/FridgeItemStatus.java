package com.example.fridgeapp.fridge;

/**
 * 冷蔵庫アイテムの状態。
 *
 * <p>消費履歴を残すため行は削除せず、消費しきった場合は {@code CONSUMED}、削除された場合は {@code DELETED} とする。一覧・編集・消費の対象は {@code
 * ACTIVE} のみ。
 */
public enum FridgeItemStatus {
  ACTIVE,
  CONSUMED,
  DELETED
}
