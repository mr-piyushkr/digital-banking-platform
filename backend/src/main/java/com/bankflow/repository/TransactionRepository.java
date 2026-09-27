package com.bankflow.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bankflow.entity.Transaction;
import com.bankflow.entity.enums.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReference(String reference);

    /** Replay detection: an existing key means the operation already ran. */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * One account's statement. Both sides are matched because a transfer stores
     * the counterparty rather than writing a second row.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId
            """)
    Page<Transaction> findByAccount(@Param("accountId") Long accountId, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId)
              AND t.createdAt BETWEEN :from AND :to
            ORDER BY t.createdAt ASC
            """)
    java.util.List<Transaction> findForStatement(
            @Param("accountId") Long accountId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /** Admin monitor. Every filter is optional, hence the null checks. */
    @Query("""
            SELECT t FROM Transaction t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:minAmount IS NULL OR t.amount >= :minAmount)
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
            """)
    Page<Transaction> monitor(
            @Param("status") TransactionStatus status,
            @Param("minAmount") BigDecimal minAmount,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /** Feeds the daily debit limit check. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.fromAccount.id = :accountId
              AND t.status IN ('SUCCESS', 'FLAGGED')
              AND t.createdAt >= :since
            """)
    BigDecimal sumDebitsSince(@Param("accountId") Long accountId, @Param("since") Instant since);

    /** Velocity rule input — counted in SQL rather than by loading rows. */
    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.fromAccount.id = :accountId AND t.createdAt >= :since
            """)
    long countDebitsSince(@Param("accountId") Long accountId, @Param("since") Instant since);

    long countByStatus(TransactionStatus status);

    long countByCreatedAtAfter(Instant since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.createdAt >= :since AND t.status = 'SUCCESS'")
    BigDecimal sumSuccessfulAmountSince(@Param("since") Instant since);
}
