package com.bankflow.dto.request;

import java.math.BigDecimal;

import com.bankflow.entity.enums.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(
        @NotNull(message = "Account type is required") AccountType type,

        /**
         * Optional opening deposit. A SAVINGS account still has to reach its
         * minimum balance before it can be debited, which AccountService checks.
         */
        @DecimalMin(value = "0.00", message = "Opening balance cannot be negative")
        @Digits(integer = 15, fraction = 4, message = "Amount has too many digits")
        BigDecimal openingBalance) {
}
