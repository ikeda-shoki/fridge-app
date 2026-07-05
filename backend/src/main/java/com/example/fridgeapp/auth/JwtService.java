package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.common.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final AppProperties.Jwt jwtProps;

  public JwtService(AppProperties props) {
    this.jwtProps = props.jwt();
    // JWT_SECRET は 256bit(32バイト)以上のランダム文字列を要求
    this.key = Keys.hmacShaKeyFor(jwtProps.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String issueAccessToken(UUID userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtProps.accessTokenExpiry().toMillis());
    return Jwts.builder()
        .subject(userId.toString())
        .issuer(jwtProps.issuer())
        .audience()
        .add(jwtProps.audience())
        .and()
        .issuedAt(now)
        .expiration(expiry)
        .id(UUID.randomUUID().toString())
        .signWith(key)
        .compact();
  }

  public UUID validateAndExtractUserId(String token) {
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(jwtProps.issuer())
              .build()
              .parseSignedClaims(token)
              .getPayload();

      if (!claims.getAudience().contains(jwtProps.audience())) {
        throw new AuthException(AppError.INVALID_TOKEN);
      }
      return UUID.fromString(claims.getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      throw new AuthException(AppError.INVALID_TOKEN);
    }
  }
}
