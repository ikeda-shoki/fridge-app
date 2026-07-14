package com.example.fridgeapp.common;

import com.example.fridgeapp.auth.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** JPA 監査（created_at / created_by / updated_at / updated_by の自動設定）を有効化する。 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

  /**
   * 監査列に記録する操作ユーザーを、認証済みリクエストの {@link AuthenticatedUser} から解決する。
   *
   * <p>未認証のリクエストや起動時のデータ投入など、認証情報がない文脈では空を返し、監査列は null のままとなる。
   */
  @Bean
  public AuditorAware<UUID> auditorAware() {
    return () ->
        Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getPrincipal)
            .filter(p -> p instanceof AuthenticatedUser)
            .map(p -> ((AuthenticatedUser) p).userId());
  }
}
