package com.example.trellite.dto;

import com.example.trellite.enums.BoardRole;

import java.time.LocalDateTime;
import java.util.Set;

public record BoardResponseDTO(
        Integer boardId,
        String boardName,
        String boardDescription,
        LocalDateTime createdAt,
        UserSummaryDTO owner,
        Set<UserSummaryDTO> members,
        // What the caller of this request is to the board, so a client can hide
        // owner-only controls instead of offering them and collecting a 403.
        BoardRole role
) {
}
