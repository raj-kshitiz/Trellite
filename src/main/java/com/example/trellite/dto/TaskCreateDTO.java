package com.example.trellite.dto;

import com.example.trellite.enums.Priority;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskCreateDTO(
        @NotBlank(message = "Title cannot be empty")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        String taskDescription,

        // Optional on the wire: the service falls back to MEDIUM, since the column is NOT NULL.
        Priority priority,

        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate startDate,

        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate deadline
) {
}
