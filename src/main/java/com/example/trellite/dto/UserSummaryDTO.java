package com.example.trellite.dto;

public record UserSummaryDTO(
        Long userId,
        String username,
        String email
) {
}
