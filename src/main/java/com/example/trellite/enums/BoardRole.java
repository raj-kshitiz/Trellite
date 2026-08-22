package com.example.trellite.enums;

/**
 * What the *current viewer* is to a board. Sent on every board response so the client
 * can hide owner-only controls instead of offering them and collecting a 403.
 */
public enum BoardRole {
    OWNER,
    MEMBER
}
