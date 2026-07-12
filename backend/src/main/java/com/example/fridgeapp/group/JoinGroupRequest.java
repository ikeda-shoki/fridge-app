package com.example.fridgeapp.group;

import jakarta.validation.constraints.NotBlank;

public record JoinGroupRequest(@NotBlank String code) {}
