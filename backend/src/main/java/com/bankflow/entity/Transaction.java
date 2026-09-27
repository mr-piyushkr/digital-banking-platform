package com.bankflow.entity;

import java.math.BigDecimal;

import com.bankflow.entity.enums.TransactionStatus;
import com.bankflow.entity.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "transactions",
        indexes = {
            @Index(name = "idx_txn_from_created", columnList = "from_account_id, created_at"),
            @Index(name = "idx_txn_to_created", columnList = "to_account_id, created_at"),
            @Index(name = "idx_txn_status", columnList = "status"),
            @Index(name = "idx_txn_created", columnList = "created_at")
        })
public class Transaction extends BaseEntity {

    /** Customer-facing identifier, e.g. TXN20260928A7F3K2. Shown in statements. */
    @Column(name = "reference", nullable = false, unique = true, length = 32)
    private String reference;

    /**
     * Supplied by the client per logical operation. The unique constraint is what
     * actually makes a retried request safe — two concurrent replays cannot both
     * insert, so one fails at the database rather than double-charging.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Null for a deposit. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    /** Null for a withdrawal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    /**
     * Balance of the account this transaction is reported against, after the
     * transaction. Stored so a statement reproduces exactly what the customer
     * saw, without recomputing history.
     */
    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    /** Set by the fraud consumer when a rule matches. */
    @Column(name = "flag_reason", length = 255)
    private String flagReason;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;
}
