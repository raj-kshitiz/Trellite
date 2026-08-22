package com.example.trellite.dto;

public record LoginResponseDTO(
        String accessToken,
        String refreshToken
) {}
