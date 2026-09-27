package com.bankflow.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.response.PageResponse;
import com.bankflow.dto.response.UserResponse;
import com.bankflow.entity.User;
import com.bankflow.exception.ResourceNotFoundException;
import com.bankflow.repository.UserRepository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Admin user management. Deliberately has no endpoint to change a customer's
 * password or move money on their behalf — separation of duties.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = search == null || search.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.search(search.trim(), pageable);

        return ResponseEntity.ok(PageResponse.from(users, UserResponse::from));
    }

    @GetMapping("/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserResponse> detail(@PathVariable Long userId) {
        return ResponseEntity.ok(UserResponse.from(require(userId)));
    }

    @PostMapping("/{userId}/enable")
    @Transactional
    public ResponseEntity<UserResponse> enable(@PathVariable Long userId) {
        User user = require(userId);
        user.setEnabled(true);
        // Unblocking should also clear a lockout, or the customer stays locked
        // out for reasons unrelated to the admin action.
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/{userId}/disable")
    @Transactional
    public ResponseEntity<UserResponse> disable(@PathVariable Long userId) {
        User user = require(userId);
        user.setEnabled(false);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    private User require(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
