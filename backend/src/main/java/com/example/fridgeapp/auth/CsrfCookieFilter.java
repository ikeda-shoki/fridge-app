package com.example.fridgeapp.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF トークンを毎リクエストで Cookie（XSRF-TOKEN）へ発行させるフィルター。
 *
 * <p>CsrfFilter は検証要否に関わらず CsrfToken をリクエスト属性として遅延ロードするだけで、実際に値を読み出さない限り Cookie への書き込みは発生しない。ここで
 * {@code csrfToken.getToken()} を呼び出すことで、認証状態や HTTP メソッドを問わず Cookie を発行させる（SPA
 * が初回アクセス時にトークンを取得できるようにするため）。
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      csrfToken.getToken();
    }
    filterChain.doFilter(request, response);
  }
}
