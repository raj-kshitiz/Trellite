package com.example.trellite.exception;

/**
 * The request is well-formed but collides with the current state — a username or email
 * already taken, a user already on the board. Maps to 409 rather than the blanket 500
 * a bare RuntimeException used to produce.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
