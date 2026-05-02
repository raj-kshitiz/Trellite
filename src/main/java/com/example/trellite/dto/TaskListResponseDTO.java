package com.example.trellite.dto;

import java.time.LocalDateTime;

public record TaskListResponseDTO(
        Integer listId,
        String listName,
        Integer position,
        LocalDateTime createdAt        // just the ID, not the full board object
) {}
