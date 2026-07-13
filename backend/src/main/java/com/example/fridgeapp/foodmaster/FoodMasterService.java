package com.example.fridgeapp.foodmaster;

import com.example.fridgeapp.common.LikeEscaper;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodMasterService {

  /** サジェストとして一度に返す最大件数。 */
  private static final Limit SUGGEST_LIMIT = Limit.of(20);

  private final FoodMasterRepository foodMasterRepository;

  public FoodMasterService(FoodMasterRepository foodMasterRepository) {
    this.foodMasterRepository = foodMasterRepository;
  }

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
