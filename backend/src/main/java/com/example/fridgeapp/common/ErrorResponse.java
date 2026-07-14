package com.example.fridgeapp.common;

/**
 * エラーレスポンスの共通形式。
 *
 * <p>{@code code} は業務エラーでは {@link AppError} の enum 名、システムエラー（5xx）では {@code SYSTEM_ERROR} 固定。
 */
public record ErrorResponse(String code, String message) {}
