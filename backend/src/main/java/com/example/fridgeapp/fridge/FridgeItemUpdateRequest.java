package com.example.fridgeapp.fridge;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * アイテム編集（FRG-02）。PATCH のため、null の項目は「変更しない」を意味する。
 *
 * @param category 食材マスタと同じ日本語ラベル（例: 野菜）
 */
public record FridgeItemUpdateRequest(
    UUID foodMasterId,
    @Size(max = 100) String displayName,
    @DecimalMin("0") BigDecimal quantity,
    @Size(max = 20) String unit,
    String category,
    LocalDate expiresAt,
    LocalDate purchasedAt,
    UUID purchasedBy,
    String memo) {}
