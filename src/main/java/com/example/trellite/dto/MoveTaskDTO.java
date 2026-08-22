package com.example.trellite.dto;

import jakarta.validation.constraints.NotNull;

public record MoveTaskDTO(
        @NotNull(message = "Target list id is required")
        Integer listId
) {
}
