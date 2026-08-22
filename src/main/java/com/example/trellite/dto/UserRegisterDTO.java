package com.example.trellite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterDTO(
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Enter a valid email address")
        String email
) {
}
