package com.example.fridgeapp.fridge;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * アイテム登録（FRG-01）。
 *
 * @param foodMasterId マスタから選択した場合のみ設定する（自由入力時は null）
 * @param category 食材マスタと同じ日本語ラベル（例: 野菜）
 * @param purchasedAt 未指定の場合は登録日
 * @param purchasedBy 未指定の場合は登録ユーザー
 */
public record FridgeItemCreateRequest(
    UUID foodMasterId,
    @NotBlank @Size(max = 100) String displayName,
    @NotNull @DecimalMin("0") BigDecimal quantity,
    @Size(max = 20) String unit,
    @NotBlank String category,
    LocalDate expiresAt,
    LocalDate purchasedAt,
    UUID purchasedBy,
    String memo) {}
