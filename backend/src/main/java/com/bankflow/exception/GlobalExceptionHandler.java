package com.bankflow.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.bankflow.dto.response.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns every exception into an ApiError. Nothing below leaks a stack trace or
 * a SQL fragment to the client — those go to the log instead.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> onBusiness(BusinessException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getCode().getStatus())
                .body(ApiError.of(ex.getCode().name(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onInvalidBody(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(ApiError.withFields(
                        ErrorCode.VALIDATION_FAILED.name(),
                        "Some fields are invalid.",
                        fields,
                        request.getRequestURI()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> onInvalidParams(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(ApiError.of(
                        ErrorCode.VALIDATION_FAILED.name(),
                        "Some request parameters are invalid.",
                        request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onUnreadableBody(HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.MALFORMED_REQUEST.getStatus())
                .body(ApiError.of(
                        ErrorCode.MALFORMED_REQUEST.name(),
                        "Request body could not be parsed.",
                        request.getRequestURI()));
    }

    /** Deliberately vague: distinguishing the two would enumerate valid emails. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> onBadCredentials(HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.getStatus())
                .body(ApiError.of(
                        ErrorCode.INVALID_CREDENTIALS.name(),
                        "Email or password is incorrect.",
                        request.getRequestURI()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> onDisabled(HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.ACCOUNT_DISABLED.getStatus())
                .body(ApiError.of(
                        ErrorCode.ACCOUNT_DISABLED.name(),
                        "This account has been disabled. Contact support.",
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> onAccessDenied(HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(ApiError.of(
                        ErrorCode.ACCESS_DENIED.name(),
                        "You do not have permission to perform this action.",
                        request.getRequestURI()));
    }

    /**
     * Two concurrent writers hit the same row. The client can safely retry, so
     * this is a 409 rather than a 500.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> onOptimisticLock(HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.CONCURRENT_MODIFICATION.getStatus())
                .body(ApiError.of(
                        ErrorCode.CONCURRENT_MODIFICATION.name(),
                        "This record changed while you were working on it. Try again.",
                        request.getRequestURI()));
    }

    /**
     * Usually a unique-constraint race that the service-level check could not
     * see. The constraint name is logged but never returned.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> onIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(ErrorCode.DUPLICATE_RESOURCE.getStatus())
                .body(ApiError.of(
                        ErrorCode.DUPLICATE_RESOURCE.name(),
                        "That record already exists.",
                        request.getRequestURI()));
    }

    /** Lets the SPA's client-side routes 404 without logging noise. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> onNoResource(HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(ApiError.of(
                        ErrorCode.RESOURCE_NOT_FOUND.name(),
                        "No such endpoint.",
                        request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(ErrorCode.UNEXPECTED_ERROR.getStatus())
                .body(ApiError.of(
                        ErrorCode.UNEXPECTED_ERROR.name(),
                        "Something went wrong. Please try again.",
                        request.getRequestURI()));
    }
}
