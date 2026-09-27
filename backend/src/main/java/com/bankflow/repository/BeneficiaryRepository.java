package com.bankflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bankflow.entity.Beneficiary;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /** Scoped by owner so one customer can never read another's beneficiary. */
    Optional<Beneficiary> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<Beneficiary> findByOwnerIdAndBeneficiaryAccountNumber(
            Long ownerId, String beneficiaryAccountNumber);

    boolean existsByOwnerIdAndBeneficiaryAccountNumber(
            Long ownerId, String beneficiaryAccountNumber);
}
