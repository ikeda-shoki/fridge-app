package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

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

  public ResponseCookie accessTokenCookie(String token) {
    return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/")
        .maxAge(jwtProps.accessTokenExpiry())
        .build();
  }

  public ResponseCookie refreshTokenCookie(String token) {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/v1/auth") // リフレッシュ・ログアウト以外には送信しない
        .maxAge(jwtProps.refreshTokenExpiry())
        .build();
  }

  public ResponseCookie clearAccessTokenCookie() {
    return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ZERO)
        .build();
  }

  public ResponseCookie clearRefreshTokenCookie() {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/v1/auth")
        .maxAge(Duration.ZERO)
        .build();
  }

  public Optional<String> extractAccessToken(HttpServletRequest request) {
    return extractCookie(request, ACCESS_TOKEN_COOKIE);
  }

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
