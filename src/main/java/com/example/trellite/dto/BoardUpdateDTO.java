package com.example.trellite.dto;

import jakarta.validation.constraints.Size;

/** Partial update — every field is optional, and null means "leave this alone". */
public record BoardUpdateDTO(
        @Size(min = 1, max = 100, message = "Board name cannot be empty")
        String boardName,

        @Size(max = 1000, message = "Board description cannot exceed 1000 characters")
        String boardDescription
) {
}
