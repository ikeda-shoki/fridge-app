package com.example.fridgeapp.auth;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import com.example.fridgeapp.common.AppProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // ステップ7以降で @PreAuthorize を使用
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CsrfCookieFilter csrfCookieFilter;
  private final AppProperties.Cors corsProperties;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      CsrfCookieFilter csrfCookieFilter,
      AppProperties appProperties) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.csrfCookieFilter = csrfCookieFilter;
    this.corsProperties = appProperties.cors();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    CsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
        new CsrfTokenRequestAttributeHandler();
    return http.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfTokenRequestHandler)
                    // ID トークン自体が本人性の証明になり、ログイン時点では
                    // まだ XSRF-TOKEN Cookie が存在しないため対象外とする
                    .ignoringRequestMatchers("/api/v1/auth/google"))
        .authorizeHttpRequests(
            auth ->
                // /error は permitAll にしないと、CSRF 拒否等で Tomcat が /error へ
                // 内部フォワードした際に匿名ユーザーが 401 で弾かれ、本来のステータス
                // コード（403 等）が上書きされてしまう
                auth.requestMatchers("/api/v1/auth/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(csrfCookieFilter, CsrfFilter.class)
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((req, res, ex) -> res.sendError(SC_UNAUTHORIZED))
                    .accessDeniedHandler((req, res, ex) -> res.sendError(SC_FORBIDDEN)))
        .build();
  }

  private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
