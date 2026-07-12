package com.example.fridgeapp.fridge;

import com.example.fridgeapp.common.AppError;

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
