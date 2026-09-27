package com.bankflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.request.BeneficiaryRequest;
import com.bankflow.dto.response.BeneficiaryResponse;
import com.bankflow.service.BeneficiaryService;
import com.bankflow.util.RequestContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final RequestContext requestContext;

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> add(@Valid @RequestBody BeneficiaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beneficiaryService.add(requestContext.requireUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> list() {
        return ResponseEntity.ok(beneficiaryService.list(requestContext.requireUserId()));
    }

    @PostMapping("/{beneficiaryId}/verify")
    public ResponseEntity<BeneficiaryResponse> verify(@PathVariable Long beneficiaryId) {
        return ResponseEntity.ok(
                beneficiaryService.verify(requestContext.requireUserId(), beneficiaryId));
    }

    @DeleteMapping("/{beneficiaryId}")
    public ResponseEntity<Void> delete(@PathVariable Long beneficiaryId) {
        beneficiaryService.delete(requestContext.requireUserId(), beneficiaryId);
        return ResponseEntity.noContent().build();
    }
}
