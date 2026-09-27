package com.bankflow.exception;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(ErrorCode code, String message) {
        super(code, message);
    }
}
