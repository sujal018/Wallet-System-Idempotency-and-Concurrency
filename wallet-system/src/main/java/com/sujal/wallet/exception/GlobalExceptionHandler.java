package com.sujal.wallet.exception;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sujal.wallet.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Handle custom business exceptions
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {

        return build(
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                request,
                ex.getDetails()
        );
    }

    // 2. Handle DTO validation errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request,
                details
        );
    }

    // 3. Handle malformed JSON or invalid request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "Malformed or unreadable request body",
                request,
                List.of()
        );
    }

    // 4. Handle missing required request headers or parameters
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ErrorResponse> handleRequestBinding(
            ServletRequestBindingException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "MISSING_REQUEST_VALUE",
                "Required request header or parameter is missing",
                request,
                List.of()
        );
    }

    // 5. Handle invalid query parameter types
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Invalid value for parameter: " + ex.getName(),
                request,
                List.of()
        );
    }

    // 6. Handle optimistic/pessimistic concurrency failures
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrency(
            ConcurrencyFailureException ex,
            HttpServletRequest request) {

        log.warn(
                "Concurrency failure on {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return build(
                HttpStatus.CONFLICT,
                "CONCURRENCY_CONFLICT",
                "Concurrent update conflict. Please retry the request",
                request,
                List.of()
        );
    }

    // 7. Handle database constraint violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn(
                "Database integrity violation on {}: {}",
                request.getRequestURI(),
                ex.getMostSpecificCause().getMessage()
        );

        return build(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "Request conflicts with existing data",
                request,
                List.of()
        );
    }

    // 8. Handle unsupported HTTP methods
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "HTTP method is not supported for this endpoint",
                request,
                List.of()
        );
    }

    // 9. Handle missing endpoints/resources
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Requested resource was not found",
                request,
                List.of()
        );
    }

    // 10. Wrong email / password (thrown by AuthenticationManager during login)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid email or password",
                request,
                List.of()
        );
    }

    // 11. Any other authentication failure that reaches the controller layer
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                "Authentication failed",
                request,
                List.of()
        );
    }

    // 12. Role check failed inside a controller (@PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to access this resource",
                request,
                List.of()
        );
    }

    // 13. Handle all unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error(
                "Unexpected error on {}",
                request.getRequestURI(),
                ex
        );

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred",
                request,
                List.of()
        );
    }

    // Common response builder
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<String> details) {

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                details
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}