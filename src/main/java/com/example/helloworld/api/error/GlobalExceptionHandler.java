package com.example.helloworld.api.error;

import com.example.helloworld.exception.GreetingNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Consistent RFC 9457 (Problem Details) error payloads for the whole API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI NOT_FOUND_TYPE = URI.create("https://api.example.com/problems/not-found");
    private static final URI VALIDATION_TYPE = URI.create("https://api.example.com/problems/validation");

    @ExceptionHandler(GreetingNotFoundException.class)
    public ProblemDetail handleNotFound(GreetingNotFoundException e) {
        ProblemDetail problem = base(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setType(NOT_FOUND_TYPE);
        problem.setTitle("Resource not found");
        return problem;
    }

    @ExceptionHandler({ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class})
    public ProblemDetail handleValidation(Exception e) {
        ProblemDetail problem = base(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Invalid request");
        problem.setProperty("detailMessage", e.getMessage());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        // Never leak internals to the caller.
        ProblemDetail problem = base(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal error");
        return problem;
    }

    private ProblemDetail base(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", MDC.get("correlationId"));
        return problem;
    }
}
