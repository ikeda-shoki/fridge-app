package com.example.fridgeapp.auth;

import jakarta.validation.constraints.NotBlank;

/** Google ログインのリクエスト。{@code idToken} はフロントで Google から取得した ID トークン。 */
public record GoogleLoginRequest(@NotBlank String idToken) {}
