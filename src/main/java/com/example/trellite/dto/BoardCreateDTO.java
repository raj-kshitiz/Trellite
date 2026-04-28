package com.example.trellite.dto;

import jakarta.validation.constraints.NotBlank;
public record BoardCreateDTO(
        @NotBlank(message = "Board name cannot be empty")
        String boardName,
        String boardDescription
) {}
