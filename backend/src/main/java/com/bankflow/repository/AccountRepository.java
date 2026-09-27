package com.bankflow.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bankflow.entity.Account;
import com.bankflow.entity.enums.AccountStatus;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    Optional<Account> findByAccountNumberAndUserId(String accountNumber, Long userId);

    long countByUserId(Long userId);

    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    /**
     * Issues SELECT ... FOR UPDATE, so a concurrent debit on the same account
     * blocks until this transaction commits. Without this, two withdrawals that
     * read the same balance could both succeed and overdraw the account.
     *
     * Must be called inside a transaction, and callers that lock two accounts
     * must do so in a deterministic order — see TransferService.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.status = 'ACTIVE'")
    BigDecimal sumActiveBalances();
}
