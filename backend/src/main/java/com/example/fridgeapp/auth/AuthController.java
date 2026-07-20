package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.group.GroupResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
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

  /**
   * CSRF トークンを初期化する。引数の {@link CsrfToken} を解決して返すことで {@code XSRF-TOKEN} Cookie を確実に発行する（Spring 公式の
   * CSRF エンドポイントパターン）。
   *
   * <p>SPA がログインや状態変更 POST を行う前にこれを 1 度呼ぶことで、最初の POST から CSRF トークンを送れるようにする。ログイン {@code
   * /auth/google} は CSRF 検証対象外で Cookie を発行せず、また CsrfCookieFilter による成功レスポンスへの Cookie
   * 付与は遅延保存の都合で不安定なため、SPA は起動時にこのエンドポイントを明示的に呼んで確実に priming する。認証は不要。
   */
  @GetMapping("/csrf")
  public CsrfToken primeCsrf(CsrfToken csrfToken) {
    return csrfToken;
  }

  /** Google ID トークンでログインし、トークン Cookie を設定してユーザー情報を返す。 */
  @PostMapping("/google")
  public ResponseEntity<UserResponse> loginWithGoogle(
      @Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {
    LoginResult result = authService.loginWithGoogle(request.idToken());
    setTokenCookies(response, result.tokens());
    List<GroupResponse> groups = authService.getUserGroups(result.user().getId());
    return ResponseEntity.ok(UserResponse.from(result.user(), groups));
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
