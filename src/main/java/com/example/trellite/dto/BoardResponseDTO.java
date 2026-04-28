package com.example.trellite.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record BoardResponseDTO<UserSummaryDTO>(
        Integer boardId,
        String boardName,
        String boardDescription,
        LocalDateTime createdAt,
        UserSummaryDTO owner,
        Set<UserSummaryDTO> members
) {
}
