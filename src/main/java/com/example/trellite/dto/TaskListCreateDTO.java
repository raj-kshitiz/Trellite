package com.example.trellite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TaskListCreateDTO(
        @NotBlank(message = "List name cannot be empty")
        @Size(max = 100, message = "List name cannot exceed 100 characters")
        String listName,

        // NOT NULL in the DB, so it has to be caught here or Postgres catches it as a 500.
        @NotNull(message = "Position is required")
        @PositiveOrZero(message = "Position cannot be negative")
        Integer position
) {
}
