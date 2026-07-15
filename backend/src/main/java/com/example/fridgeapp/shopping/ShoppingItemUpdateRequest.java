package com.example.fridgeapp.shopping;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** アイテム編集（SHP-02 のチェック切り替えを含む）。PATCH のため、null の項目は「変更しない」を意味する。 */
public record ShoppingItemUpdateRequest(
    UUID foodMasterId,
    @Size(max = 100) String displayName,
    @DecimalMin("0") BigDecimal quantity,
    String memo,
    Boolean checked) {}
