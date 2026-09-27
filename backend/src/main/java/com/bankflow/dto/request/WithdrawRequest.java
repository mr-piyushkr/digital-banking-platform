package com.bankflow.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WithdrawRequest(
        @NotNull(message = "Account number is required")
        @Size(min = 5, max = 20, message = "Enter a valid account number")
        String accountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Minimum withdrawal is 1.00")
        @DecimalMax(value = "10000000.00", message = "Maximum withdrawal per transaction is 1,00,00,000")
        @Digits(integer = 15, fraction = 4, message = "Amount has too many digits")
        BigDecimal amount,

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description) {
}
