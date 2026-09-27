package com.bankflow.dto.response;

import java.time.Instant;

import com.bankflow.entity.Beneficiary;

public record BeneficiaryResponse(
        Long id,
        String beneficiaryAccountNumber,
        String beneficiaryName,
        String nickname,
        String displayName,
        String bankIfsc,
        boolean verified,
        Instant createdAt) {

    public static BeneficiaryResponse from(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getBeneficiaryAccountNumber(),
                beneficiary.getBeneficiaryName(),
                beneficiary.getNickname(),
                beneficiary.getDisplayName(),
                beneficiary.getBankIfsc(),
                beneficiary.isVerified(),
                beneficiary.getCreatedAt());
    }
}
