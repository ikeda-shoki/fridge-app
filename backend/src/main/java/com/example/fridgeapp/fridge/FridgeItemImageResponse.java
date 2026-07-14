package com.example.fridgeapp.fridge;

/** 画像アップロードのレスポンス。{@code imagePath} はストレージ実装が返す不透明なパス。 */
public record FridgeItemImageResponse(String imagePath) {}
