package com.example.fridgeapp.common;

/**
 * LIKE 検索でユーザー入力をリテラルとして扱うためのエスケープ処理。
 *
 * <p>ユーザー入力をそのまま LIKE のパターンに埋め込むと、{@code %} や {@code _} がワイルドカードとして解釈され、意図しない検索結果を返してしまう。検索語を LIKE
 * パターンへ組み込む前に必ず {@link #escape(String)} を通すこと。
 *
 * <p>エスケープ文字は {@code \} を使うため、JPQL 側でも {@code ESCAPE '\'} を明示する。
 *
 * <pre>{@code
 * // Repository
 * @Query("""
 *     SELECT i FROM FridgeItem i
 *     WHERE i.displayName LIKE CONCAT('%', :keyword, '%') ESCAPE '\\'
 *     """)
 * List<FridgeItem> searchByDisplayName(@Param("keyword") String keyword);
 *
 * // Service
 * repository.searchByDisplayName(LikeEscaper.escape(input));
 * }</pre>
 */
public final class LikeEscaper {

  private LikeEscaper() {}

  /**
   * 検索語に含まれる LIKE のワイルドカード（{@code %} {@code _}）とエスケープ文字（{@code \}）自身をエスケープする。
   *
   * @param input ユーザーが入力した検索語
   * @return LIKE パターンに安全に埋め込める文字列
   */
  public static String escape(String input) {
    return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
