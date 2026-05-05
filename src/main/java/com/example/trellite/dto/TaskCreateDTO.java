package com.example.trellite.dto;

import com.example.trellite.enums.Priority;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record TaskCreateDTO(
        String title,
        String taskDescription,

        Priority priority,

        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate startDate,

        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate deadline
) {
}
