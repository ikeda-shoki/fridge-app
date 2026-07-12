package com.example.fridgeapp.group;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** 招待コード用の6桁英数字を生成する。紛らわしい 0/O/1/I は除外する。 */
@Component
public class InvitationCodeGenerator {

  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 6;

  private final SecureRandom random = new SecureRandom();

  public String generate() {
    StringBuilder sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return sb.toString();
  }
}
