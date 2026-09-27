package com.bankflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.request.OpenAccountRequest;
import com.bankflow.dto.response.AccountResponse;
import com.bankflow.service.AccountService;
import com.bankflow.util.RequestContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final RequestContext requestContext;

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.openAccount(requestContext.requireUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> myAccounts() {
        return ResponseEntity.ok(accountService.listOwnAccounts(requestContext.requireUserId()));
    }

    /**
     * The guard is belt-and-braces: the service query is already scoped by the
     * authenticated user id, so a foreign account cannot be returned even if the
     * annotation were removed.
     */
    @GetMapping("/{accountId}")
    @PreAuthorize("@accountGuard.owns(#accountId)")
    public ResponseEntity<AccountResponse> byId(@PathVariable Long accountId) {
        return ResponseEntity.ok(
                accountService.getOwnAccount(requestContext.requireUserId(), accountId));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> byNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(
                accountService.getOwnAccountByNumber(requestContext.requireUserId(), accountNumber));
    }
}
