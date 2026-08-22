package com.example.trellite.exception;

/**
 * The task-drafting dependency failed to produce a card — the upstream model call errored,
 * or the model returned something unusable. Deliberately carries a fixed, client-safe
 * message: upstream provider errors must not be echoed to callers.
 */
public class AiDraftException extends RuntimeException {
    public AiDraftException(String message) {
        super(message);
    }

    public AiDraftException(String message, Throwable cause) {
        super(message, cause);
    }
}
