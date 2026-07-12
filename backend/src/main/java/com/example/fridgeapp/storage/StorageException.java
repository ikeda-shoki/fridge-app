package com.example.fridgeapp.storage;

import com.example.fridgeapp.common.AppError;

public class StorageException extends RuntimeException {

  private final AppError error;

  public StorageException(AppError error) {
    super(error.getMessage());
    this.error = error;
  }

  public StorageException(AppError error, Throwable cause) {
    super(error.getMessage(), cause);
    this.error = error;
  }

  public AppError getError() {
    return error;
  }
}
