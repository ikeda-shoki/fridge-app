package com.example.fridgeapp.shopping;

import com.example.fridgeapp.common.AppError;

/**
 * 買い物リストの業務例外。
 *
 * <p>HTTP ステータスへの対応は {@link com.example.fridgeapp.common.GlobalExceptionHandler} を参照。
 */
public class ShoppingItemException extends RuntimeException {

  private final AppError error;

  public ShoppingItemException(AppError error) {
    super(error.getMessage());
    this.error = error;
  }

  public AppError getError() {
    return error;
  }
}
