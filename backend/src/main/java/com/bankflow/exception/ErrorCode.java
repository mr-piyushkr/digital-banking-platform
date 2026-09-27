package com.bankflow.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable machine-readable codes. The frontend switches on these rather than on
 * message text, so wording can change without breaking a client.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),

    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN),

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),

    DUPLICATE_EMAIL(HttpStatus.CONFLICT),
    DUPLICATE_PHONE(HttpStatus.CONFLICT),
    DUPLICATE_BENEFICIARY(HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),

    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY),
    ACCOUNT_BLOCKED(HttpStatus.UNPROCESSABLE_ENTITY),
    ACCOUNT_CLOSED(HttpStatus.UNPROCESSABLE_ENTITY),
    BENEFICIARY_NOT_VERIFIED(HttpStatus.UNPROCESSABLE_ENTITY),
    SAME_ACCOUNT_TRANSFER(HttpStatus.UNPROCESSABLE_ENTITY),
    DAILY_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY),
    MINIMUM_BALANCE_BREACH(HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_OPERATION(HttpStatus.UNPROCESSABLE_ENTITY),

    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),

    UNEXPECTED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
