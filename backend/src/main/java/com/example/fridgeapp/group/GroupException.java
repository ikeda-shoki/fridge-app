package com.example.fridgeapp.group;

import com.example.fridgeapp.common.AppError;

/**
 * グループ・招待の業務例外。
 *
 * <p>グループ配下のリソース（冷蔵庫アイテム等）に対する認可エラーもこの例外で表す。HTTP ステータスへの対応は {@link
 * com.example.fridgeapp.common.GlobalExceptionHandler} を参照。
 */
public class GroupException extends RuntimeException {

  private final AppError error;

  public GroupException(AppError error) {
    super(error.getMessage());
    this.error = error;
  }

  public AppError getError() {
    return error;
  }
}
