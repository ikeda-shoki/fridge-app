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
  GROUP_NOT_FOUND("グループが見つかりません"),
  NOT_GROUP_MEMBER("このグループのメンバーではありません"),
  NOT_GROUP_OWNER("この操作はオーナーのみ実行できます"),
  ALREADY_GROUP_MEMBER("既にこのグループのメンバーです"),
  TARGET_USER_NOT_GROUP_MEMBER("譲渡先のユーザーはこのグループのメンバーではありません"),
  LAST_OWNER_CANNOT_LEAVE("唯一のオーナーは脱退できません。オーナーを譲渡するかグループを削除してください"),
  INVALID_INVITATION_CODE("無効な招待コードです"),
  INVITATION_CODE_EXPIRED("招待コードの有効期限が切れています"),
  INVITATION_CODE_ALREADY_USED("この招待コードは既に使用されています"),
  INVITATION_CODE_LOCKED("この招待コードはロックされています。しばらくたってから再度お試しください"),
  JOIN_RATE_LIMITED("リクエストが多すぎます。しばらくたってから再度お試しください"),

  // ── Fridge Items ──────────────────────────────────────────────────────
  FRIDGE_ITEM_NOT_FOUND("冷蔵庫アイテムが見つかりません"),
  FRIDGE_ITEM_NOT_ACTIVE("このアイテムは既に消費済みまたは削除されています"),
  INVALID_FRIDGE_ITEM_CATEGORY("対応していないカテゴリです"),
  CORRUPTED_FRIDGE_ITEM_CATEGORY("保存されたカテゴリの値が不正です"),
  INSUFFICIENT_QUANTITY("消費数量がアイテムの残数を超えています"),
  INVALID_IMAGE_FORMAT("対応していない画像形式です（JPEG/PNGのみ）"),
  IMAGE_TOO_LARGE("画像サイズが5MBを超えています"),
  IMAGE_PROCESSING_FAILED("画像の処理に失敗しました"),

  // ── Storage ───────────────────────────────────────────────────────────
  STORAGE_DIRECTORY_CREATE_FAILED("ストレージディレクトリの作成に失敗しました"),
  STORAGE_WRITE_FAILED("ファイルの保存に失敗しました"),
  STORAGE_DELETE_FAILED("ファイルの削除に失敗しました"),
  STORAGE_INVALID_PATH("不正なパスです"),

  // ── Shopping List ─────────────────────────────────────────────────────
  // (ステップ10で追加予定)

  // ── Food Master ───────────────────────────────────────────────────────
  FOOD_MASTER_NOT_FOUND("指定された食材マスタが見つかりません"),
  ;

  private final String message;

  AppError(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
