package com.example.trellite.dto;

/** A null assigneeId is meaningful here: it clears the card's assignee. */
public record AssignTaskDTO(
        Long assigneeId
) {
}
