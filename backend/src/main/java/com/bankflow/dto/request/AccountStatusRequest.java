package com.bankflow.dto.request;

import com.bankflow.entity.enums.AccountStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountStatusRequest(
        @NotNull(message = "Status is required") AccountStatus status,

        @Size(max = 255, message = "Reason cannot exceed 255 characters") String reason) {
}
