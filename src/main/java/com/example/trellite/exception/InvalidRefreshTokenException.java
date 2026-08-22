package com.example.trellite.exception;

/**
 * The presented refresh token is unknown, expired, or has already been rotated away.
 * Maps to 401 so the client clears its session and sends the user back to login,
 * rather than the 404 an expired token used to produce.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
