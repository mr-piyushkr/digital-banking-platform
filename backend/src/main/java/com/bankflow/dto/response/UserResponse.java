package com.bankflow.dto.response;

import java.time.Instant;
import java.util.List;

import com.bankflow.entity.Role;
import com.bankflow.entity.User;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        boolean enabled,
        String kycStatus,
        String addressLine,
        String city,
        String state,
        String pincode,
        List<String> roles,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.isEnabled(),
                user.getKycStatus().name(),
                user.getAddressLine(),
                user.getCity(),
                user.getState(),
                user.getPincode(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).sorted().toList(),
                user.getCreatedAt());
    }
}
