package com.bankflow.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.request.DepositRequest;
import com.bankflow.dto.request.TransferRequest;
import com.bankflow.dto.request.WithdrawRequest;
import com.bankflow.dto.response.PageResponse;
import com.bankflow.dto.response.TransactionResponse;
import com.bankflow.service.TransactionService;
import com.bankflow.service.TransferService;
import com.bankflow.util.RequestContext;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    /**
     * Clients send this so a retry after a timeout cannot charge twice. It is
     * optional — when absent the server generates one, which still protects
     * against a double-submit within the same request but not across retries.
     */
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;
    private final TransferService transferService;
    private final RequestContext requestContext;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(
                        requestContext.requireUserId(), request, idempotencyKey));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdraw(
                        requestContext.requireUserId(), request, idempotencyKey));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.transfer(
                        requestContext.requireUserId(), request, idempotencyKey));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("@accountGuard.owns(#accountId)")
    public ResponseEntity<PageResponse<TransactionResponse>> history(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        Page<TransactionResponse> result = transactionService.history(
                requestContext.requireUserId(),
                accountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(PageResponse.from(result, txn -> txn));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<TransactionResponse> byReference(@PathVariable String reference) {
        return ResponseEntity.ok(
                transactionService.getOwnTransaction(requestContext.requireUserId(), reference));
    }
}
