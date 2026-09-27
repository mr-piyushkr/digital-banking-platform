package com.bankflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.request.ChangePasswordRequest;
import com.bankflow.dto.request.UpdateProfileRequest;
import com.bankflow.dto.response.UserResponse;
import com.bankflow.service.AuthService;
import com.bankflow.service.UserService;
import com.bankflow.util.RequestContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Everything here acts on the caller's own record — the id comes from the token,
 * never from the path, so there is no identifier for a client to tamper with.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final RequestContext requestContext;

    @GetMapping
    public ResponseEntity<UserResponse> profile() {
        return ResponseEntity.ok(userService.getProfile(requestContext.requireUserId()));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                userService.updateProfile(requestContext.requireUserId(), request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(requestContext.requireUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
