package com.bankflow.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.repository.AccountRepository;
import com.bankflow.util.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Second authorization layer, referenced from @PreAuthorize as
 * {@code @accountGuard.owns(#accountId)}.
 *
 * The URL rules in SecurityConfig cannot express "this account belongs to the
 * caller" — that needs a row lookup. Services additionally use owner-scoped
 * repository queries, so a missed annotation still cannot leak another
 * customer's account.
 */
@Slf4j
@Component("accountGuard")
@RequiredArgsConstructor
public class AccountGuard {

    private final AccountRepository accountRepository;
    private final RequestContext requestContext;

    @Transactional(readOnly = true)
    public boolean owns(Long accountId) {
        if (accountId == null) {
            return false;
        }
        return requestContext
                .currentUserId()
                .map(userId -> {
                    boolean owned = accountRepository.findByIdAndUserId(accountId, userId).isPresent();
                    if (!owned) {
                        log.warn("User {} attempted to access account {}", userId, accountId);
                    }
                    return owned;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ownsAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return false;
        }
        return requestContext
                .currentUserId()
                .map(userId -> accountRepository
                        .findByAccountNumberAndUserId(accountNumber, userId)
                        .isPresent())
                .orElse(false);
    }
}
