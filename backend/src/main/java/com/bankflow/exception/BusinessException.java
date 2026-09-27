package com.bankflow.exception;

import lombok.Getter;

/**
 * Base for every expected domain failure. Carries an ErrorCode so the handler
 * can map it to a status without a chain of instanceof checks.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
