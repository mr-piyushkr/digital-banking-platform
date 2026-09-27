package com.bankflow.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.request.AccountStatusRequest;
import com.bankflow.dto.response.AccountResponse;
import com.bankflow.dto.response.PageResponse;
import com.bankflow.entity.Account;
import com.bankflow.entity.enums.AccountStatus;
import com.bankflow.service.AccountService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AccountService accountService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<AccountResponse>> list(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Account> accounts = status == null
                ? accountService.listAll(pageable)
                : accountService.listByStatus(status, pageable);

        return ResponseEntity.ok(PageResponse.from(accounts, AccountResponse::from));
    }

    @PostMapping("/{accountId}/status")
    public ResponseEntity<AccountResponse> changeStatus(
            @PathVariable Long accountId, @Valid @RequestBody AccountStatusRequest request) {
        return ResponseEntity.ok(
                accountService.changeStatus(accountId, request.status(), request.reason()));
    }
}
