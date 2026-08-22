package com.example.trellite.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials. The endpoint used to bind the User *entity* straight from the
 * request body, which meant the JSON could reach fields (role, id, createdAt) that a
 * caller has no business setting. Wire shape is unchanged: {username, password}.
 */
public record LoginRequestDTO(
        @NotBlank(message = "Username cannot be empty")
        String username,

        @NotBlank(message = "Password cannot be empty")
        String password
) {
}
