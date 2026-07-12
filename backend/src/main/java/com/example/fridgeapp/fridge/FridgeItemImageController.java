package com.example.fridgeapp.fridge;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/fridge-items/{id}/image")
public class FridgeItemImageController {

  private final FridgeItemImageService fridgeItemImageService;

  public FridgeItemImageController(FridgeItemImageService fridgeItemImageService) {
    this.fridgeItemImageService = fridgeItemImageService;
  }

  @PostMapping(consumes = "multipart/form-data")
  public ResponseEntity<FridgeItemImageResponse> uploadImage(
      @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
    String imagePath = fridgeItemImageService.uploadImage(id, file);
    return ResponseEntity.ok(new FridgeItemImageResponse(imagePath));
  }

  @DeleteMapping
  public ResponseEntity<Void> deleteImage(@PathVariable UUID id) {
    fridgeItemImageService.deleteImage(id);
    return ResponseEntity.noContent().build();
  }
}
