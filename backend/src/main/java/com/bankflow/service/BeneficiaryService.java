package com.bankflow.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.dto.request.BeneficiaryRequest;
import com.bankflow.dto.response.BeneficiaryResponse;
import com.bankflow.entity.Account;
import com.bankflow.entity.Beneficiary;
import com.bankflow.entity.User;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.DuplicateResourceException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.exception.ResourceNotFoundException;
import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.BeneficiaryRepository;
import com.bankflow.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public BeneficiaryResponse add(Long userId, BeneficiaryRequest request) {
        String accountNumber = request.beneficiaryAccountNumber().trim();

        if (beneficiaryRepository.existsByOwnerIdAndBeneficiaryAccountNumber(userId, accountNumber)) {
            throw new DuplicateResourceException(
                    ErrorCode.DUPLICATE_BENEFICIARY, "This account is already in your beneficiary list.");
        }

        Account target = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));

        if (target.getUser().getId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_OPERATION,
                    "Your own account does not need to be added as a beneficiary.");
        }

        User owner = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setOwner(owner);
        beneficiary.setBeneficiaryAccountNumber(accountNumber);
        beneficiary.setBeneficiaryName(request.beneficiaryName().trim());
        beneficiary.setNickname(blankToNull(request.nickname()));
        beneficiary.setBankIfsc(blankToNull(request.bankIfsc()));
        // Added unverified. The owner has to confirm before money can move,
        // which is what makes a beneficiary list a safety feature rather than
        // just an address book.
        beneficiary.setVerified(false);

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        log.info("User {} added beneficiary {}", userId, accountNumber);
        return BeneficiaryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> list(Long userId) {
        return beneficiaryRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(BeneficiaryResponse::from)
                .toList();
    }

    /**
     * Stands in for the confirmation step a real bank would send by SMS or
     * email. The important property is that it is a separate, deliberate action
     * from adding.
     */
    @Transactional
    public BeneficiaryResponse verify(Long userId, Long beneficiaryId) {
        Beneficiary beneficiary = requireOwned(userId, beneficiaryId);
        beneficiary.setVerified(true);
        log.info("User {} verified beneficiary {}", userId, beneficiaryId);
        return BeneficiaryResponse.from(beneficiary);
    }

    @Transactional
    public void delete(Long userId, Long beneficiaryId) {
        beneficiaryRepository.delete(requireOwned(userId, beneficiaryId));
        log.info("User {} deleted beneficiary {}", userId, beneficiaryId);
    }

    private Beneficiary requireOwned(Long userId, Long beneficiaryId) {
        return beneficiaryRepository
                .findByIdAndOwnerId(beneficiaryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", beneficiaryId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
