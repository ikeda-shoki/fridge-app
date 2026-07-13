package com.example.fridgeapp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LikeEscaperTest {

  @Test
  void escapesPercentSign() {
    assertThat(LikeEscaper.escape("100%")).isEqualTo("100\\%");
  }

  @Test
  void escapesUnderscore() {
    assertThat(LikeEscaper.escape("a_b")).isEqualTo("a\\_b");
  }

  @Test
  void escapesBackslashItself() {
    assertThat(LikeEscaper.escape("a\\b")).isEqualTo("a\\\\b");
  }

  @Test
  void escapesBackslashBeforeWildcardsSoEscapeCharacterIsNotDoubledIncorrectly() {
    assertThat(LikeEscaper.escape("\\%")).isEqualTo("\\\\\\%");
  }

  @Test
  void leavesPlainTextUnchanged() {
    assertThat(LikeEscaper.escape("たまねぎ")).isEqualTo("たまねぎ");
  }
}
