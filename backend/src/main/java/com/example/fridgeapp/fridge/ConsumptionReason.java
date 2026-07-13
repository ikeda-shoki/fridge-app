package com.example.fridgeapp.fridge;

/** 消費理由。RECIPE は将来のレシピ連携用（MVP では MANUAL / EXPIRED のみ利用）。 */
public enum ConsumptionReason {
  MANUAL,
  RECIPE,
  EXPIRED
}
