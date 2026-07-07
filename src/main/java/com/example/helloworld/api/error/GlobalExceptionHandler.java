package com.example.helloworld.api.error;

import com.example.helloworld.exception.GreetingNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Consistent RFC 9457 (Problem Details) error payloads for the whole API.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so every standard MVC
 * exception (unknown path → 404, unsupported method → 405, unreadable body
 * → 400, handler-method validation → 400, …) is also rendered as a
 * ProblemDetail instead of falling into the generic 500 handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI NOT_FOUND_TYPE = URI.create("https://api.example.com/problems/not-found");
    private static final URI VALIDATION_TYPE = URI.create("https://api.example.com/problems/validation");

    @ExceptionHandler(GreetingNotFoundException.class)
    public ProblemDetail handleNotFound(GreetingNotFoundException e) {
        ProblemDetail problem = enrich(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage()));
        problem.setType(NOT_FOUND_TYPE);
        problem.setTitle("Resource not found");
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        ProblemDetail problem = enrich(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed"));
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Invalid request");
        problem.setProperty("errors", e.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "field", String.valueOf(v.getPropertyPath()),
                        "message", String.valueOf(v.getMessage())))
                .toList());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        // Never leak internals (exception class, message, stack frames) to the caller.
        ProblemDetail problem = enrich(
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
        problem.setTitle("Internal error");
        return problem;
    }

    /** Body (@Valid) validation: structured per-field errors, no framework internals. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ex.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Invalid request");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", String.valueOf(fe.getDefaultMessage())))
                .toList());
        return createResponseEntity(problem, headers, status, request);
    }

    /** Adds shared metadata to every ProblemDetail built by the parent handler as well. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body,
                                                          HttpHeaders headers,
                                                          HttpStatusCode statusCode,
                                                          WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            enrich(problem);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ProblemDetail enrich(ProblemDetail problem) {
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", MDC.get("correlationId"));
        return problem;
    }
}
