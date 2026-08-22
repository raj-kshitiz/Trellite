package com.example.trellite.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

/**
 * Every response body here is plain text and is written to be shown to a person as-is —
 * the frontend renders it verbatim next to the field that failed.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    // The caller is signed in but this is not theirs. Previously a bare RuntimeException,
    // so every permission failure in the app surfaced as a 500.
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbiddenException(ForbiddenException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
    }

    // Duplicate username/email, someone already on the board.
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflictException(ConflictException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
    }

    // 401, not 404: an unknown or expired refresh token means "start a new session".
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<String> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // AuthenticationManager.authenticate() throws rather than returning an
    // unauthenticated token, and AuthenticationException is a RuntimeException, so
    // without this a wrong password would fall through to the handler below as a 500.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException(AuthenticationException e) {
        return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
    }

    // 502 rather than 500: the failure is in an upstream dependency, not this service.
    // The message comes from AiDraftException, which is always a fixed string — the
    // provider's own error text is logged in AiTaskService, never returned.
    @ExceptionHandler(AiDraftException.class)
    public ResponseEntity<String> handleAiDraftException(AiDraftException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    /** @Valid failures on a request body — reported field by field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            // Record-level checks (@AssertTrue) land in the global errors, not field errors.
            message = e.getBindingResult().getGlobalErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .distinct()
                    .collect(Collectors.joining("; "));
        }
        return new ResponseEntity<>(message.isBlank() ? "Invalid request" : message, HttpStatus.BAD_REQUEST);
    }

    /** @Validated failures on query parameters, e.g. an empty ?q= on user search. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(message.isBlank() ? "Invalid request" : message, HttpStatus.BAD_REQUEST);
    }

    /** Malformed JSON, or a value that is not one of the allowed enum constants. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.debug("Unreadable request body", e);
        return new ResponseEntity<>("Could not read the request — check the fields and try again",
                HttpStatus.BAD_REQUEST);
    }

    /** A path variable or query parameter of the wrong type, e.g. /boards/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return new ResponseEntity<>("'" + e.getValue() + "' is not a valid " + e.getName(),
                HttpStatus.BAD_REQUEST);
    }

    /** A DB constraint the service checks did not catch — a race on a unique column. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Database constraint violated", e);
        return new ResponseEntity<>("That conflicts with something that already exists",
                HttpStatus.CONFLICT);
    }

    /**
     * Anything unplanned. The message is deliberately generic: it used to return
     * e.getMessage(), which put internal exception text (and whatever it quoted) in front
     * of the user. The real detail goes to the log.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        log.error("Unhandled exception", e);
        return new ResponseEntity<>("Something went wrong on our end", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
