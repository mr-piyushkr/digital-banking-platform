package com.bankflow.exception;

import com.bankflow.entity.enums.AccountStatus;

public class AccountBlockedException extends BusinessException {

    public AccountBlockedException(String accountNumber, AccountStatus status) {
        super(
                status == AccountStatus.CLOSED ? ErrorCode.ACCOUNT_CLOSED : ErrorCode.ACCOUNT_BLOCKED,
                "Account " + accountNumber + " is " + status.name().toLowerCase()
                        + " and cannot be used for this operation.");
    }
}
