package com.bankflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.dto.request.UpdateProfileRequest;
import com.bankflow.dto.response.UserResponse;
import com.bankflow.entity.User;
import com.bankflow.exception.DuplicateResourceException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.exception.ResourceNotFoundException;
import com.bankflow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(requireUser(userId));
    }

    /**
     * Email is intentionally not editable here — changing a login identifier
     * needs a verification flow, and silently allowing it would let a user take
     * over an address they do not control.
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);

        if (!user.getPhone().equals(request.phone())
                && userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException(
                    ErrorCode.DUPLICATE_PHONE, "That mobile number is already in use.");
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setAddressLine(blankToNull(request.addressLine()));
        user.setCity(blankToNull(request.city()));
        user.setState(blankToNull(request.state()));
        user.setPincode(blankToNull(request.pincode()));

        return UserResponse.from(user);
    }

    private User requireUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
