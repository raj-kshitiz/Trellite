package com.example.trellite.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Partial update — every field is optional, and null means "leave this alone". */
public record TaskListUpdateDTO(
        @Size(min = 1, max = 100, message = "List name cannot be empty")
        String listName,

        @PositiveOrZero(message = "Position cannot be negative")
        Integer position
) {
}
