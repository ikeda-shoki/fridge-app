package com.example.fridgeapp.auth;

/** ログイン結果。トークンは Cookie へ、ユーザーはレスポンスボディへ変換して返す。 */
public record LoginResult(AuthTokens tokens, User user) {}
