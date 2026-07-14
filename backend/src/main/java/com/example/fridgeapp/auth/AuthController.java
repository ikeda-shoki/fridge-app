package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認証 API（AUTH-01〜03）。ログイン・トークン更新・ログアウトを扱う。
 *
 * <p>アクセストークン／リフレッシュトークンはレスポンスボディに載せず、すべて HttpOnly Cookie で受け渡す。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final TokenCookieService tokenCookieService;

  public AuthController(AuthService authService, TokenCookieService tokenCookieService) {
    this.authService = authService;
    this.tokenCookieService = tokenCookieService;
  }

  /** Google ID トークンでログインし、トークン Cookie を設定してユーザー情報を返す。 */
  @PostMapping("/google")
  public ResponseEntity<UserResponse> loginWithGoogle(
      @Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {
    LoginResult result = authService.loginWithGoogle(request.idToken());
    setTokenCookies(response, result.tokens());
    return ResponseEntity.ok(UserResponse.from(result.user()));
  }

  /**
   * リフレッシュトークン Cookie から新しいトークン一式を再発行する（リフレッシュトークンはローテーションされる）。
   *
   * @throws AuthException Cookie にリフレッシュトークンが無い場合（{@link AppError#MISSING_REFRESH_TOKEN}）
   */
  @PostMapping("/refresh")
  public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
    String rawRefreshToken =
        tokenCookieService
            .extractRefreshToken(request)
            .orElseThrow(() -> new AuthException(AppError.MISSING_REFRESH_TOKEN));
    AuthTokens tokens = authService.refresh(rawRefreshToken);
    setTokenCookies(response, tokens);
    return ResponseEntity.noContent().build();
  }

  /** リフレッシュトークンを失効させ、トークン Cookie を削除する。Cookie が無い場合も成功として扱う。 */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    tokenCookieService.extractRefreshToken(request).ifPresent(authService::logout);
    response.addHeader(
        HttpHeaders.SET_COOKIE, tokenCookieService.clearAccessTokenCookie().toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE, tokenCookieService.clearRefreshTokenCookie().toString());
    return ResponseEntity.noContent().build();
  }

  private void setTokenCookies(HttpServletResponse response, AuthTokens tokens) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        tokenCookieService.accessTokenCookie(tokens.accessToken()).toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        tokenCookieService.refreshTokenCookie(tokens.refreshToken()).toString());
  }
}
