package com.example.fridgeapp.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(@Valid Jwt jwt, @Valid Google google, @Valid Cookie cookie) {

  public record Jwt(
      @NotBlank String secret,
      @NotBlank String issuer,
      @NotBlank String audience,
      @NotNull Duration accessTokenExpiry,
      @NotNull Duration refreshTokenExpiry) {}

  public record Google(@NotBlank String clientId) {}

  public record Cookie(boolean secure) {}
}
