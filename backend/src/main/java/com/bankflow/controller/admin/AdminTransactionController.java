package com.bankflow.controller.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.response.PageResponse;
import com.bankflow.dto.response.TransactionResponse;
import com.bankflow.entity.Transaction;
import com.bankflow.entity.enums.TransactionStatus;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.exception.ResourceNotFoundException;
import com.bankflow.repository.TransactionRepository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Transaction monitoring. The GET endpoints are also reachable by ROLE_AUDITOR
 * (see SecurityConfig); the review action is admin-only, enforced here at method
 * level as well as by the URL rule.
 */
@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final TransactionRepository transactionRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<TransactionResponse>> monitor(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Instant fromInstant = from == null ? null : from.atStartOfDay(ZONE).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant();

        var result = transactionRepository.monitor(
                status,
                minAmount,
                fromInstant,
                toInstant,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(PageResponse.from(result, TransactionResponse::forAdmin));
    }

    @GetMapping("/{reference}")
    @Transactional(readOnly = true)
    public ResponseEntity<TransactionResponse> detail(@PathVariable String reference) {
        return ResponseEntity.ok(TransactionResponse.forAdmin(require(reference)));
    }

    /**
     * Clears a fraud flag after a human has looked at it. The transaction moves
     * back to SUCCESS; the flag reason is kept so the audit trail still shows
     * that it was reviewed rather than never flagged.
     */
    @PostMapping("/{reference}/clear-flag")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<TransactionResponse> clearFlag(@PathVariable String reference) {
        Transaction txn = require(reference);

        if (txn.getStatus() != TransactionStatus.FLAGGED) {
            throw new BusinessException(
                    ErrorCode.INVALID_OPERATION, "Only a flagged transaction can be cleared.");
        }

        txn.setStatus(TransactionStatus.SUCCESS);
        return ResponseEntity.ok(TransactionResponse.forAdmin(txn));
    }

    private Transaction require(String reference) {
        return transactionRepository
                .findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", reference));
    }
}
