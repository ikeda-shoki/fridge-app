package com.example.fridgeapp.group;

import com.example.fridgeapp.common.AppError;

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
