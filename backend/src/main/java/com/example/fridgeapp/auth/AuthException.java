package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;

public class AuthException extends RuntimeException {

  private final AppError error;

  public AuthException(AppError error) {
    super(error.getMessage());
    this.error = error;
  }

  public String getCode() {
    return error.name();
  }
}
