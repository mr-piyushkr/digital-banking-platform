package com.bankflow.entity;

import java.math.BigDecimal;

import com.bankflow.entity.enums.AccountStatus;
import com.bankflow.entity.enums.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "accounts",
        indexes = {
            @Index(name = "idx_accounts_user", columnList = "user_id"),
            @Index(name = "idx_accounts_status", columnList = "status")
        })
public class Account extends BaseEntity {

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * DECIMAL(19,4) — never a floating-point type. Four decimal places leaves
     * room for interest calculations that are not whole paise.
     */
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    /**
     * Optimistic lock. Transfers additionally take a pessimistic row lock, so
     * this is the second line of defence against a lost update.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public boolean canDebit(BigDecimal amount) {
        if (!status.allowsDebit()) {
            return false;
        }
        BigDecimal remaining = balance.subtract(amount);
        return remaining.compareTo(type.getMinimumBalance()) >= 0;
    }

    public void debit(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    /** Balance minus the type's minimum — what the customer can actually spend. */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(type.getMinimumBalance()).max(BigDecimal.ZERO);
    }
}
