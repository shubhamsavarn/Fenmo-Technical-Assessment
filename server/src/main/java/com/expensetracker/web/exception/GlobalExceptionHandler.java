package com.expensetracker.web.exception;

import com.expensetracker.application.exception.IdempotencyConflictException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Global exception handler producing RFC 7807 Problem Details responses.
 *
 * <p>Extends ResponseEntityExceptionHandler to get pre-built handling for
 * standard Spring MVC exceptions (missing params, wrong content type, etc.),
 * then adds custom handlers for domain exceptions.</p>
 *
 * <p>Critical principle: <b>never leak stack traces or internal details in production</b>.
 * All internal errors are logged server-side with full context, but the client
 * only sees a sanitized message.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean validation errors (e.g., @NotBlank, @Pattern failed).
     * Returns 400 with field-level violation details.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Error");
        problem.setDetail("Request body contains invalid fields");

        List<Map<String, String>> violations = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid"
                ))
                .toList();

        problem.setProperty("violations", violations);

        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Idempotency key reused with different payload → 409 Conflict.
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Idempotency Conflict");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * Invalid Money amount or date format.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid Request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * Catch-all for unexpected errors.
     * Log full stack trace server-side, return sanitized message to client.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred. Please try again later.");
        return problem;
    }
}
