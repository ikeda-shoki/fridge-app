package com.example.fridgeapp.common;

import com.example.fridgeapp.auth.AuthException;
import com.example.fridgeapp.fridge.FridgeItemException;
import com.example.fridgeapp.group.GroupException;
import com.example.fridgeapp.shopping.ShoppingItemException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 例外を HTTP レスポンスへ一元的に変換する。
 *
 * <p>業務例外は {@link AppError} をもとに 4xx とユーザー向けメッセージを返す。想定外の例外は 500 +
 * 固定メッセージとし、内部情報をレスポンスに含めない（詳細はログにのみ残す）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final ErrorResponse SYSTEM_ERROR =
      new ErrorResponse("SYSTEM_ERROR", "システムエラーが発生しました。しばらくたってから再度お試しください");

  /** 認証エラーはすべて 401 とする。 */
  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  /** 冷蔵庫アイテムの業務エラーを、エラー種別に応じた 4xx へ変換する。 */
  @ExceptionHandler(FridgeItemException.class)
  public ResponseEntity<ErrorResponse> handleFridgeItemException(FridgeItemException ex) {
    HttpStatus status =
        switch (ex.getError()) {
          case FRIDGE_ITEM_NOT_FOUND, FOOD_MASTER_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case FRIDGE_ITEM_NOT_ACTIVE -> HttpStatus.CONFLICT;
          case IMAGE_PROCESSING_FAILED -> HttpStatus.UNPROCESSABLE_CONTENT;
          default -> HttpStatus.BAD_REQUEST;
        };
    return ResponseEntity.status(status)
        .body(new ErrorResponse(ex.getError().name(), ex.getMessage()));
  }

  /** 買い物リストの業務エラーを、エラー種別に応じた 4xx へ変換する。 */
  @ExceptionHandler(ShoppingItemException.class)
  public ResponseEntity<ErrorResponse> handleShoppingItemException(ShoppingItemException ex) {
    HttpStatus status =
        switch (ex.getError()) {
          case SHOPPING_ITEM_NOT_FOUND, FOOD_MASTER_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case SHOPPING_ITEM_NOT_CHECKED -> HttpStatus.CONFLICT;
          default -> HttpStatus.BAD_REQUEST;
        };
    return ResponseEntity.status(status)
        .body(new ErrorResponse(ex.getError().name(), ex.getMessage()));
  }

  /** グループ・招待の業務エラーを、エラー種別に応じた 4xx へ変換する（認可エラーは 403）。 */
  @ExceptionHandler(GroupException.class)
  public ResponseEntity<ErrorResponse> handleGroupException(GroupException ex) {
    HttpStatus status =
        switch (ex.getError()) {
          case GROUP_NOT_FOUND, TARGET_USER_NOT_GROUP_MEMBER -> HttpStatus.NOT_FOUND;
          case NOT_GROUP_MEMBER, NOT_GROUP_OWNER -> HttpStatus.FORBIDDEN;
          case ALREADY_GROUP_MEMBER, LAST_OWNER_CANNOT_LEAVE -> HttpStatus.CONFLICT;
          case INVITATION_CODE_LOCKED, JOIN_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
          default -> HttpStatus.BAD_REQUEST;
        };
    return ResponseEntity.status(status)
        .body(new ErrorResponse(ex.getError().name(), ex.getMessage()));
  }

  /** アップロードサイズ上限（Spring 側で弾かれる分）を、画像サイズ超過の業務エラーとして 400 で返す。 */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex) {
    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                AppError.IMAGE_TOO_LARGE.name(), AppError.IMAGE_TOO_LARGE.getMessage()));
  }

  /** Bean Validation の違反を、項目名つきのメッセージにまとめて 400 で返す。 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", message));
  }

  /** 想定外の例外は 500 + 固定メッセージで返し、原因はログにのみ残す（内部情報を漏らさないため）。 */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(SYSTEM_ERROR);
  }
}
