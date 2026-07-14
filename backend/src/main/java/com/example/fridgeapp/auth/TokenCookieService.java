package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * 認証トークンを載せる Cookie の生成・削除・読み取りを担う。
 *
 * <p>いずれも HttpOnly（JS から読めない＝XSS でトークンを奪われない）・SameSite=Lax。リフレッシュトークンは path を {@code /api/v1/auth}
 * に限定し、通常の API リクエストには送信させない。
 */
@Service
public class TokenCookieService {

  static final String ACCESS_TOKEN_COOKIE = "access_token";
  static final String REFRESH_TOKEN_COOKIE = "refresh_token";

  private final boolean cookieSecure;
  private final AppProperties.Jwt jwtProps;

  public TokenCookieService(AppProperties props) {
    this.cookieSecure = props.cookie().secure();
    this.jwtProps = props.jwt();
  }

  /** アクセストークン Cookie を生成する。有効期限はアクセストークンの有効期限に合わせる。 */
  public ResponseCookie accessTokenCookie(String token) {
    return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/")
        .maxAge(jwtProps.accessTokenExpiry())
        .build();
  }

  /** リフレッシュトークン Cookie を生成する。送信先を認証系エンドポイントのみに絞る。 */
  public ResponseCookie refreshTokenCookie(String token) {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/v1/auth") // リフレッシュ・ログアウト以外には送信しない
        .maxAge(jwtProps.refreshTokenExpiry())
        .build();
  }

  /** アクセストークン Cookie を削除する（ログアウト用）。 */
  public ResponseCookie clearAccessTokenCookie() {
    return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ZERO)
        .build();
  }

  /** リフレッシュトークン Cookie を削除する（ログアウト用）。生成時と path を揃えないと削除されない点に注意。 */
  public ResponseCookie clearRefreshTokenCookie() {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/v1/auth")
        .maxAge(Duration.ZERO)
        .build();
  }

  /** リクエストからアクセストークンを取り出す。Cookie が無ければ空。 */
  public Optional<String> extractAccessToken(HttpServletRequest request) {
    return extractCookie(request, ACCESS_TOKEN_COOKIE);
  }

  /** リクエストからリフレッシュトークンを取り出す。Cookie が無ければ空。 */
  public Optional<String> extractRefreshToken(HttpServletRequest request) {
    return extractCookie(request, REFRESH_TOKEN_COOKIE);
  }

  private Optional<String> extractCookie(HttpServletRequest request, String name) {
    if (request.getCookies() == null) return Optional.empty();
    return Arrays.stream(request.getCookies())
        .filter(c -> name.equals(c.getName()))
        .map(c -> c.getValue())
        .findFirst();
  }
}
