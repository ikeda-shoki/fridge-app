package com.example.fridgeapp.common;

public enum AppError {

  // ── Common ────────────────────────────────────────────────────────────
  USER_NOT_FOUND("ユーザーが見つかりません"),

  // ── Auth ──────────────────────────────────────────────────────────────
  ACCOUNT_DELETED("このアカウントは削除されています"),
  INVALID_TOKEN("無効なアクセストークンです"),
  INVALID_GOOGLE_TOKEN("Google IDトークンの検証に失敗しました"),
  MISSING_REFRESH_TOKEN("リフレッシュトークンが見つかりません"),
  INVALID_REFRESH_TOKEN("リフレッシュトークンが無効または期限切れです"),

// ── Groups ────────────────────────────────────────────────────────────
// (ステップ7で追加予定)

// ── Fridge Items ──────────────────────────────────────────────────────
// (ステップ9で追加予定)

// ── Shopping List ─────────────────────────────────────────────────────
// (ステップ10で追加予定)

// ── Food Master ───────────────────────────────────────────────────────
// (ステップ8で追加予定)
;

  private final String message;

  AppError(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
