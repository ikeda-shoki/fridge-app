package com.example.fridgeapp.auth;

/** 発行したトークン一式。いずれも Cookie に格納する内部用の値で、レスポンスボディには含めない。 */
public record AuthTokens(String accessToken, String refreshToken) {}
