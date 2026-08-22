package com.example.trellite.exception;

/**
 * The caller is authenticated but is not allowed to touch this resource — they are
 * neither the owner nor a member of the board it belongs to. Distinct from
 * {@link ResourceNotFoundException}: the resource exists, the caller just cannot have it.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
