package com.example.trellite.dto;

import com.example.trellite.enums.Priority;
import com.example.trellite.enums.TaskStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record TaskResponseDTO(
        Integer taskId,
        String title,
        String taskDescription,
        TaskStatus status,
        Priority priority,

        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate startDate,

        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate deadline,

        Integer taskListId,
        UserSummaryDTO assignee
) {
}
