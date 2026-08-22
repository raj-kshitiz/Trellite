package com.example.trellite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardCreateDTO(
        @NotBlank(message = "Board name cannot be empty")
        @Size(max = 100, message = "Board name cannot exceed 100 characters")
        String boardName,

        @Size(max = 1000, message = "Board description cannot exceed 1000 characters")
        String boardDescription
) {
}
