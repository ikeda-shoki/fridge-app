package com.example.fridgeapp.common;

import com.example.fridgeapp.auth.AuthException;
import com.example.fridgeapp.fridge.FridgeItemException;
import com.example.fridgeapp.group.GroupException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final ErrorResponse SYSTEM_ERROR =
      new ErrorResponse("SYSTEM_ERROR", "システムエラーが発生しました。しばらくたってから再度お試しください");

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(FridgeItemException.class)
  public ResponseEntity<ErrorResponse> handleFridgeItemException(FridgeItemException ex) {
    HttpStatus status =
        switch (ex.getError()) {
          case FRIDGE_ITEM_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case IMAGE_PROCESSING_FAILED -> HttpStatus.UNPROCESSABLE_CONTENT;
          default -> HttpStatus.BAD_REQUEST;
        };
    return ResponseEntity.status(status)
        .body(new ErrorResponse(ex.getError().name(), ex.getMessage()));
  }

  @ExceptionHandler(GroupException.class)
  public ResponseEntity<ErrorResponse> handleGroupException(GroupException ex) {
    HttpStatus status =
        switch (ex.getError()) {
          case GROUP_NOT_FOUND, TARGET_USER_NOT_GROUP_MEMBER -> HttpStatus.NOT_FOUND;
          case NOT_GROUP_MEMBER, NOT_GROUP_OWNER -> HttpStatus.FORBIDDEN;
          case ALREADY_GROUP_MEMBER, LAST_OWNER_CANNOT_LEAVE -> HttpStatus.CONFLICT;
          default -> HttpStatus.BAD_REQUEST;
        };
    return ResponseEntity.status(status)
        .body(new ErrorResponse(ex.getError().name(), ex.getMessage()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex) {
    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                AppError.IMAGE_TOO_LARGE.name(), AppError.IMAGE_TOO_LARGE.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(SYSTEM_ERROR);
  }
}
