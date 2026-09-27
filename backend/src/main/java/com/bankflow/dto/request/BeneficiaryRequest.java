package com.bankflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BeneficiaryRequest(
        @NotBlank(message = "Beneficiary account number is required")
        @Size(min = 5, max = 20, message = "Enter a valid account number")
        String beneficiaryAccountNumber,

        @NotBlank(message = "Beneficiary name is required")
        @Size(max = 120, message = "Name cannot exceed 120 characters")
        String beneficiaryName,

        @Size(max = 60, message = "Nickname cannot exceed 60 characters")
        String nickname,

        @Pattern(regexp = "^$|^[A-Z]{4}0[A-Z0-9]{6}$", message = "Enter a valid IFSC code")
        String bankIfsc) {
}
