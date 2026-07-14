package com.example.fridgeapp.foodmaster;

import com.example.fridgeapp.common.LikeEscaper;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 食材マスタの検索（サジェスト）を扱う。 */
@Service
public class FoodMasterService {

  /** サジェストとして一度に返す最大件数。 */
  private static final Limit SUGGEST_LIMIT = Limit.of(20);

  private final FoodMasterRepository foodMasterRepository;

  public FoodMasterService(FoodMasterRepository foodMasterRepository) {
    this.foodMasterRepository = foodMasterRepository;
  }

  /**
   * よみがなの前方一致で有効な食材を検索する（MST-02）。よみがな昇順で、サジェスト用に最大 20 件まで返す。
   *
   * <p>検索語が空・空白のみの場合は、全件返さず空リストを返す。
   */
  @Transactional(readOnly = true)
  public List<FoodMasterResponse> search(String query) {
    String trimmed = query == null ? "" : query.trim();
    if (trimmed.isEmpty()) {
      return List.of();
    }
    return foodMasterRepository
        .findActiveByNameKanaPrefix(LikeEscaper.escape(trimmed), SUGGEST_LIMIT)
        .stream()
        .map(FoodMasterResponse::from)
        .toList();
  }
}
