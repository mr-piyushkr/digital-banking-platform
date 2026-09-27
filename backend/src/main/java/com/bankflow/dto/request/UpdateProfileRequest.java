package com.bankflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 60, message = "First name cannot exceed 60 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 60, message = "Last name cannot exceed 60 characters")
        String lastName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,

        @Size(max = 200) String addressLine,
        @Size(max = 80) String city,
        @Size(max = 80) String state,

        @Pattern(regexp = "^$|^\\d{6}$", message = "Enter a valid 6-digit PIN code")
        String pincode) {
}
