package com.bankflow.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.response.StatementResponse;
import com.bankflow.service.StatementService;
import com.bankflow.util.RequestContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final StatementService statementService;
    private final RequestContext requestContext;

    /**
     * Dates are accepted as plain days in the bank's timezone and widened to
     * cover the whole of the end day, which is what a customer means by
     * "up to the 30th".
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("@accountGuard.owns(#accountId)")
    public ResponseEntity<StatementResponse> statement(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Instant fromInstant = from.atStartOfDay(ZONE).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZONE).toInstant();

        return ResponseEntity.ok(statementService.generate(
                requestContext.requireUserId(), accountId, fromInstant, toInstant));
    }
}
