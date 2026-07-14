package com.example.fridgeapp.fridge;

import com.example.fridgeapp.common.AppError;

/**
 * 冷蔵庫アイテムの業務例外。
 *
 * <p>HTTP ステータスへの対応は {@link com.example.fridgeapp.common.GlobalExceptionHandler} を参照。
 */
public class FridgeItemException extends RuntimeException {

  private final AppError error;

  public FridgeItemException(AppError error) {
    super(error.getMessage());
    this.error = error;
  }

  public AppError getError() {
    return error;
  }
}
