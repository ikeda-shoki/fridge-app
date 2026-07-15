package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;

/** 認証の業務例外。{@link com.example.fridgeapp.common.GlobalExceptionHandler} で 401 に変換される。 */
public class AuthException extends RuntimeException {

  private final AppError error;

  public AuthException(AppError error) {
    super(error.getMessage());
    this.error = error;
  }

  public String getCode() {
    return error.name();
  }

  public AppError getError() {
    return error;
  }
}
