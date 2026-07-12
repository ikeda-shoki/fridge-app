package com.example.fridgeapp.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupCreateRequest(@NotBlank @Size(max = 100) String name) {}
