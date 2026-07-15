package com.example.fridgeapp.shopping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 冷蔵庫への移動（SHP-04）。数量・食材マスタ参照・メモは買い物リストアイテムから引き継ぐため、賞味期限・カテゴリなど冷蔵庫アイテム側でのみ必要な項目を受け取る。
 *
 * @param category 食材マスタと同じ日本語ラベル（例: 野菜）
 * @param purchasedAt 未指定の場合は登録日
 * @param purchasedBy 未指定の場合は登録ユーザー
 */
public record MoveToFridgeRequest(
    @NotBlank String category,
    LocalDate expiresAt,
    @Size(max = 20) String unit,
    LocalDate purchasedAt,
    UUID purchasedBy) {}
