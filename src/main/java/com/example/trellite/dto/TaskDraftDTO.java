package com.example.trellite.dto;

import com.example.trellite.enums.Priority;

/**
 * The shape the model fills in. Spring AI derives a JSON schema from this record,
 * so the component names below are effectively part of the prompt.
 * <p>
 * Dates are Strings, not LocalDate, on purpose: the model emits ISO yyyy-MM-dd and
 * {@code AiTaskService} parses it. The REST-facing DTOs use a dd-MM-yyyy
 * {@code @JsonFormat}, and that pattern is applied by the web layer's mapper, not by
 * the one Spring AI's converter uses — so binding LocalDate here would mean relying
 * on two separately-configured mappers agreeing. Parsing one known format is simpler.
 */
public record TaskDraftDTO(
        String title,
        String taskDescription,
        Priority priority,
        String startDate,
        String deadline
) {
}
