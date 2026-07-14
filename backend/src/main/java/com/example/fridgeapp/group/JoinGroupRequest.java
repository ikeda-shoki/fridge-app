package com.example.fridgeapp.group;

import jakarta.validation.constraints.NotBlank;

/** グループ参加のリクエスト。{@code code} は招待コード（大文字小文字・前後空白は Service 側で正規化する）。 */
public record JoinGroupRequest(@NotBlank String code) {}
