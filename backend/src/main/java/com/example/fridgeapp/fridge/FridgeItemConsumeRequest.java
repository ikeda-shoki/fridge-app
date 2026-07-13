package com.example.fridgeapp.fridge;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 数量消費（FRG-07）。消費すると consumption_events に履歴が記録される。
 *
 * @param reason 未指定の場合は MANUAL
 */
public record FridgeItemConsumeRequest(
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
    ConsumptionReason reason) {}
