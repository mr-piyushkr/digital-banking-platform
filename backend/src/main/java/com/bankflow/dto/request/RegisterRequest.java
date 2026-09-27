package com.bankflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 60, message = "First name cannot exceed 60 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 60, message = "Last name cannot exceed 60 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 160, message = "Email cannot exceed 160 characters")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,

        /**
         * Length is the dominant factor in password strength, so the floor is 10
         * rather than the usual 8, with one character class from each group.
         */
        @NotBlank(message = "Password is required")
        @Size(min = 10, max = 72, message = "Password must be between 10 and 72 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain an uppercase letter, a lowercase letter and a digit")
        String password,

        @Size(max = 200) String addressLine,
        @Size(max = 80) String city,
        @Size(max = 80) String state,

        @Pattern(regexp = "^$|^\\d{6}$", message = "Enter a valid 6-digit PIN code")
        String pincode) {
}
