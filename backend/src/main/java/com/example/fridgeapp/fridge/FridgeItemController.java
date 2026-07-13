package com.example.fridgeapp.fridge;

import com.example.fridgeapp.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FridgeItemController {

  private final FridgeItemService fridgeItemService;

  public FridgeItemController(FridgeItemService fridgeItemService) {
    this.fridgeItemService = fridgeItemService;
  }

  @GetMapping("/groups/{groupId}/fridge-items")
  public ResponseEntity<List<FridgeItemResponse>> listItems(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID groupId,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "q", required = false) String query) {
    return ResponseEntity.ok(
        fridgeItemService.listItems(principal.userId(), groupId, category, query));
  }

  @PostMapping("/groups/{groupId}/fridge-items")
  public ResponseEntity<FridgeItemResponse> createItem(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID groupId,
      @Valid @RequestBody FridgeItemCreateRequest request) {
    return ResponseEntity.ok(fridgeItemService.createItem(principal.userId(), groupId, request));
  }

  @PatchMapping("/fridge-items/{id}")
  public ResponseEntity<FridgeItemResponse> updateItem(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @Valid @RequestBody FridgeItemUpdateRequest request) {
    return ResponseEntity.ok(fridgeItemService.updateItem(principal.userId(), id, request));
  }

  @DeleteMapping("/fridge-items/{id}")
  public ResponseEntity<Void> deleteItem(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    fridgeItemService.deleteItem(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/fridge-items/{id}/consume")
  public ResponseEntity<FridgeItemResponse> consumeItem(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @Valid @RequestBody FridgeItemConsumeRequest request) {
    return ResponseEntity.ok(fridgeItemService.consumeItem(principal.userId(), id, request));
  }
}
