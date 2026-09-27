package com.bankflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "beneficiaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_beneficiary_owner_account",
                columnNames = {"owner_user_id", "beneficiary_account_number"}),
        indexes = @Index(name = "idx_beneficiaries_owner", columnList = "owner_user_id"))
public class Beneficiary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    /**
     * Stored as a number rather than a FK to accounts: a beneficiary can point
     * at an account that is later closed, and the record must survive that.
     */
    @Column(name = "beneficiary_account_number", nullable = false, length = 20)
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_name", nullable = false, length = 120)
    private String beneficiaryName;

    @Column(name = "nickname", length = 60)
    private String nickname;

    @Column(name = "bank_ifsc", length = 11)
    private String bankIfsc;

    /** A transfer is refused until the owner has verified the beneficiary. */
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    public String getDisplayName() {
        return nickname != null && !nickname.isBlank() ? nickname : beneficiaryName;
    }
}
