package com.example.trellite.dto;

import jakarta.validation.constraints.AssertTrue;

/**
 * Names the person a membership change is aimed at, by id or by username. Username was
 * added because a client has no way to learn someone's id before they are already on the
 * board — the only listed ids were the existing members'.
 */
public record MemberUpdateDTO(
        Long userId,
        String username
) {
    @AssertTrue(message = "Provide either a userId or a username")
    public boolean isTargetGiven() {
        return userId != null || (username != null && !username.isBlank());
    }
}
