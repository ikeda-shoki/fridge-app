package com.example.fridgeapp.shopping;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * アイテム追加（SHP-01）。
 *
 * @param foodMasterId マスタから選択した場合のみ設定する（自由入力時は null）
 */
public record ShoppingItemCreateRequest(
    UUID foodMasterId,
    @NotBlank @Size(max = 100) String displayName,
    @NotNull @DecimalMin("0") BigDecimal quantity,
    String memo) {}
