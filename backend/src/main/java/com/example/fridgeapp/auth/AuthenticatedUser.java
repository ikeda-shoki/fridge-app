package com.example.fridgeapp.auth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 認証済みユーザーを表す Principal。Controller では {@code @AuthenticationPrincipal} で受け取る。
 *
 * <p>ロールは扱わないため権限は常に空、パスワード認証もしないため password は null を返す。
 */
public record AuthenticatedUser(UUID userId) implements UserDetails {

  @Override
  public String getUsername() {
    return userId.toString();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return null;
  }
}
