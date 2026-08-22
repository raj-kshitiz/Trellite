package com.example.trellite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The plain-English request a user types to describe a card,
 * e.g. "fix the login redirect bug by Friday, high priority".
 */
public record TaskDraftRequestDTO(
        @NotBlank(message = "Prompt cannot be empty")
        @Size(max = 1000, message = "Prompt cannot exceed 1000 characters")
        String prompt
) {
}
