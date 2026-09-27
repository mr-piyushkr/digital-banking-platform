package com.bankflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.dto.request.OpenAccountRequest;
import com.bankflow.dto.response.AccountResponse;
import com.bankflow.entity.Account;
import com.bankflow.entity.User;
import com.bankflow.entity.enums.AccountStatus;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.exception.ResourceNotFoundException;
import com.bankflow.kafka.event.AccountStatusChangedEvent;
import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.UserRepository;
import com.bankflow.util.AccountNumberGenerator;
import com.bankflow.util.MoneyUtils;
import com.bankflow.util.ReferenceGenerator;
import com.bankflow.util.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    /** Keeps one customer from opening accounts without limit. */
    private static final int MAX_ACCOUNTS_PER_CUSTOMER = 5;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final ReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RequestContext requestContext;

    @Transactional
    public AccountResponse openAccount(Long userId, OpenAccountRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (accountRepository.countByUserId(userId) >= MAX_ACCOUNTS_PER_CUSTOMER) {
            throw new BusinessException(
                    ErrorCode.INVALID_OPERATION,
                    "You already have the maximum of " + MAX_ACCOUNTS_PER_CUSTOMER + " accounts.");
        }

        BigDecimal opening = MoneyUtils.normalise(
                request.openingBalance() == null ? BigDecimal.ZERO : request.openingBalance());

        Account account = new Account();
        account.setAccountNumber(accountNumberGenerator.next());
        account.setUser(user);
        account.setType(request.type());
        account.setBalance(opening);
        account.setStatus(AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);
        log.info("Opened {} account {} for user {}", request.type(), saved.getAccountNumber(), userId);

        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listOwnAccounts(Long userId) {
        return accountRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    /**
     * Owner-scoped by query, not by a check after loading. A caller passing
     * someone else's id gets a 404, and there is no code path that could return
     * the row by mistake.
     */
    @Transactional(readOnly = true)
    public AccountResponse getOwnAccount(Long userId, Long accountId) {
        return accountRepository
                .findByIdAndUserId(accountId, userId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    @Transactional(readOnly = true)
    public AccountResponse getOwnAccountByNumber(Long userId, String accountNumber) {
        return accountRepository
                .findByAccountNumberAndUserId(accountNumber, userId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public Page<Account> listAll(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Account> listByStatus(AccountStatus status, Pageable pageable) {
        return accountRepository.findByStatus(status, pageable);
    }

    /**
     * Admin-only. Emits an event so the change lands in the audit trail and the
     * customer is notified — a silent block would be indistinguishable from a bug
     * from the customer's side.
     */
    @Transactional
    public AccountResponse changeStatus(Long accountId, AccountStatus newStatus, String reason) {
        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        AccountStatus previous = account.getStatus();
        if (previous == newStatus) {
            return AccountResponse.from(account);
        }
        if (previous == AccountStatus.CLOSED) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_CLOSED, "A closed account cannot be reopened.");
        }
        if (newStatus == AccountStatus.CLOSED
                && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_OPERATION,
                    "Account cannot be closed while it holds a balance.");
        }

        account.setStatus(newStatus);

        applicationEventPublisher.publishEvent(new AccountStatusChangedEvent(
                referenceGenerator.nextEventId(),
                Instant.now(),
                account.getId(),
                account.getAccountNumber(),
                account.getUser().getId(),
                previous.name(),
                newStatus.name(),
                requestContext.currentUserId().orElse(null),
                reason,
                requestContext.clientIp(),
                requestContext.userAgent()));

        log.info("Account {} moved {} -> {}", account.getAccountNumber(), previous, newStatus);
        return AccountResponse.from(account);
    }
}
