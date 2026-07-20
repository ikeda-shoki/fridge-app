package com.example.fridgeapp.fridge;

/** 配信する画像の実体と Content-Type。レスポンスボディにそのまま載せる。 */
public record FridgeItemImageContent(byte[] content, String contentType) {}
