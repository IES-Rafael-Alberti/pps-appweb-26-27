package com.vetclinic.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.query.sqm.UnknownPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception into an <a href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807</a> problem
 * document, so clients always get a consistent error shape.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String BASE_TYPE = "https://vetclinic.example.com/problems/";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), "not-found", request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Business rule violation", ex.getMessage(), "conflict", request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), "bad-request", request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed", ex.getMessage(), "unauthorized", request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", ex.getMessage(), "forbidden", request);
    }

    /**
     * Method security ({@code @PreAuthorize}) throws this from inside the controller invocation, so without
     * an explicit handler the catch-all below would turn a 403 into a misleading 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied",
                "Your role cannot perform this action.", "forbidden", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        for (var error : ex.getBindingResult().getGlobalErrors()) {
            fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage());
        }
        return withFieldErrors(HttpStatus.BAD_REQUEST, "One or more fields are invalid", fieldErrors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                    HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            fieldErrors.putIfAbsent(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        }
        return withFieldErrors(HttpStatus.BAD_REQUEST, "One or more parameters are invalid", fieldErrors, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String detail = ex.getMostSpecificCause() instanceof IllegalArgumentException
                && ex.getMostSpecificCause().getMessage() != null
                ? ex.getMostSpecificCause().getMessage()
                : "Parameter '%s' has an invalid value: %s".formatted(ex.getName(), ex.getValue());
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter", detail, "bad-parameter", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Missing parameter",
                "Required parameter '%s' is missing.".formatted(ex.getParameterName()),
                "bad-parameter", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", describe(ex),
                "malformed-body", request);
    }

    /**
     * Jackson wraps the real cause, and for an unknown enum value that cause holds a message far more
     * useful than "parse error": it lists the accepted values. Surface it when we can.
     */
    private String describe(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof IllegalArgumentException && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return "The JSON body could not be parsed. Check field names, types and enum values.";
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(OptimisticLockingFailureException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Concurrent modification",
                "The resource was modified by another request. Re-read it and try again.",
                "concurrent-modification", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Data conflict",
                "The request violates a database constraint (duplicate or referenced record).",
                "data-conflict", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", "No endpoint matches this request.",
                "not-found", request);
    }

    // --- Errors caused by the request itself -------------------------------------------------------------------
    // These are produced by Spring MVC/Spring Data before any service runs. Without an explicit handler the
    // catch-all below would report them as 500, which is wrong: the caller sent something invalid, the server
    // is fine. Declaring them here takes precedence over the DefaultHandlerExceptionResolver.

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", ex.getMessage(),
                "method-not-allowed", request);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        String allow = ex.getSupportedHttpMethods() == null
                ? null
                : ex.getSupportedHttpMethods().stream().map(HttpMethod::name).sorted()
                        .collect(Collectors.joining(", "));
        if (allow != null) {
            response.header(HttpHeaders.ALLOW, allow);
        }
        return response.body(problem);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", ex.getMessage(),
                "unsupported-media-type", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                      HttpServletRequest request) {
        return build(HttpStatus.NOT_ACCEPTABLE, "Not acceptable", ex.getMessage(),
                "not-acceptable", request);
    }

    @ExceptionHandler({MissingPathVariableException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ProblemDetail> handleMissingRequestValue(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Missing request value", ex.getMessage(),
                "bad-parameter", request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handleUnknownSortProperty(PropertyReferenceException ex,
                                                                   HttpServletRequest request) {
        log.warn("Unknown sort property '{}' requested on {}", ex.getPropertyName(), ex.getType());
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter",
                "Cannot sort by '%s': no such property on %s.".formatted(ex.getPropertyName(), ex.getType()),
                "bad-parameter", request);
    }

    /**
     * Custom {@code @Query} methods validate the {@code Sort} against the query's select clause, so an
     * unknown column surfaces as an {@code InvalidDataAccessApiUsageException} wrapping Hibernate's
     * {@code UnknownPathException} instead of a {@code PropertyReferenceException}. Same client error, so it
     * becomes a 400. When the cause is something else the query itself is broken, which is our bug and must
     * keep surfacing as a 500 rather than being disguised as a bad request.
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ProblemDetail> handleInvalidQueryUsage(InvalidDataAccessApiUsageException ex,
                                                                 HttpServletRequest request) {
        if (!isUnknownSortColumn(ex)) {
            log.error("Invalid query usage for {}", request.getRequestURI(), ex);
            return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                    "An unexpected error occurred. Please try again later.", "internal-error", request);
        }
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter",
                "One of the requested sort properties does not exist on %s."
                        .formatted(request.getRequestURI()),
                "bad-parameter", request);
    }

    private boolean isUnknownSortColumn(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof UnknownPathException) {
                return true;
            }
            String message = t.getMessage();
            if (message != null && (message.contains("Sort expression") || message.contains("order by"))) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred. Please try again later.", "internal-error", request);
    }

    private ResponseEntity<ProblemDetail> withFieldErrors(HttpStatus status, String detail,
                                                          Map<String, String> fieldErrors,
                                                          HttpServletRequest request) {
        ProblemDetail problem = problem(status, "Validation failed", detail, "validation-error", request);
        problem.setProperty("errors", fieldErrors);
        return ResponseEntity.status(status).body(problem);
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail, String type,
                                                HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(problem(status, title, detail, type, request));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type,
                                  HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_TYPE + type));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
