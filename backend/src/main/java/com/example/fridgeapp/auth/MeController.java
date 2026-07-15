package com.example.fridgeapp.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ログイン中ユーザー情報の API（AUTH-04・AUTH-05）。 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final AuthService authService;
  private final TokenCookieService tokenCookieService;

  public MeController(AuthService authService, TokenCookieService tokenCookieService) {
    this.authService = authService;
    this.tokenCookieService = tokenCookieService;
  }

  /** ログイン中のユーザー情報を返す。 */
  @GetMapping
  public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal AuthenticatedUser principal) {
    User user = authService.getUser(principal.userId());
    return ResponseEntity.ok(UserResponse.from(user));
  }

  /** 退会する（AUTH-05）。論理削除後、トークン Cookie を削除してセッションを終了させる。 */
  @DeleteMapping
  public ResponseEntity<Void> deleteMe(
      @AuthenticationPrincipal AuthenticatedUser principal, HttpServletResponse response) {
    authService.deleteAccount(principal.userId());
    response.addHeader(
        HttpHeaders.SET_COOKIE, tokenCookieService.clearAccessTokenCookie().toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE, tokenCookieService.clearRefreshTokenCookie().toString());
    return ResponseEntity.noContent().build();
  }
}
