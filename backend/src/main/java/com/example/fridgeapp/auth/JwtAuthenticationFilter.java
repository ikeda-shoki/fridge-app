package com.example.fridgeapp.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final TokenCookieService tokenCookieService;

  public JwtAuthenticationFilter(JwtService jwtService, TokenCookieService tokenCookieService) {
    this.jwtService = jwtService;
    this.tokenCookieService = tokenCookieService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    tokenCookieService
        .extractAccessToken(request)
        .ifPresent(
            token -> {
              try {
                UUID userId = jwtService.validateAndExtractUserId(token);
                AuthenticatedUser principal = new AuthenticatedUser(userId);
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
              } catch (AuthException e) {
                // 無効なトークンは無視して未認証のままにする（以降のフィルターで 401 になる）
              }
            });

    chain.doFilter(request, response);
  }
}
