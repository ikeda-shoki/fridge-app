package com.example.fridgeapp.foodmaster;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodMasterService {

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
        .findTop20ByActiveTrueAndNameKanaStartingWithOrderByNameKanaAsc(trimmed)
        .stream()
        .map(FoodMasterResponse::from)
        .toList();
  }
}
