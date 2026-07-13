package com.example.fridgeapp.foodmaster;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/food-master")
public class FoodMasterController {

  private final FoodMasterService foodMasterService;

  public FoodMasterController(FoodMasterService foodMasterService) {
    this.foodMasterService = foodMasterService;
  }

  @GetMapping("/search")
  public ResponseEntity<List<FoodMasterResponse>> search(
      @RequestParam(name = "q", defaultValue = "") String query) {
    return ResponseEntity.ok(foodMasterService.search(query));
  }
}
