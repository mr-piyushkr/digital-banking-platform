package com.bankflow.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.bankflow.config.JpaAuditingConfig;
import com.bankflow.support.TestFlywayConfig;
import com.bankflow.entity.Account;
import com.bankflow.entity.Role;
import com.bankflow.entity.Transaction;
import com.bankflow.entity.User;
import com.bankflow.entity.enums.AccountStatus;
import com.bankflow.entity.enums.AccountType;
import com.bankflow.entity.enums.RoleName;
import com.bankflow.entity.enums.TransactionStatus;
import com.bankflow.entity.enums.TransactionType;

import jakarta.persistence.EntityManager;

/**
 * Proves the entity mapping matches the Flyway schema and that the constraints
 * the money logic depends on are actually enforced by the database — not just
 * asserted in Java.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, TestFlywayConfig.class})
@ActiveProfiles("test")
class PersistenceMappingTest {

    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private AccountRepository accounts;
    @Autowired private TransactionRepository transactions;
    @Autowired private EntityManager entityManager;

    private User customer;

    @BeforeEach
    void setUp() {
        customer = users.save(newUser("asha.mehta@example.com", "9812345670"));
    }

    @Test
    void migrationSeedsTheThreeRoles() {
        assertThat(roles.findAll())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder(
                        RoleName.ROLE_CUSTOMER, RoleName.ROLE_ADMIN, RoleName.ROLE_AUDITOR);
    }

    @Test
    void auditingPopulatesTimestamps() {
        assertThat(customer.getCreatedAt()).isNotNull();
        assertThat(customer.getUpdatedAt()).isNotNull();
    }

    @Test
    void customerCanBeAssignedARole() {
        Role role = roles.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        customer.addRole(role);
        users.saveAndFlush(customer);
        entityManager.clear();

        User reloaded = users.findById(customer.getId()).orElseThrow();
        assertThat(reloaded.hasRole(RoleName.ROLE_CUSTOMER)).isTrue();
        assertThat(reloaded.hasRole(RoleName.ROLE_ADMIN)).isFalse();
    }

    @Test
    void emailIsUniqueAcrossUsers() {
        assertThatThrownBy(() -> users.saveAndFlush(newUser("asha.mehta@example.com", "9800000001")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void balanceIsStoredWithFourDecimalPlaces() {
        Account account = accounts.saveAndFlush(newAccount("ACC0000000001", new BigDecimal("1234.5678")));
        entityManager.clear();

        Account reloaded = accounts.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("1234.5678");
        assertThat(reloaded.getBalance().scale()).isEqualTo(4);
    }

    @Test
    void accountNumberIsUnique() {
        accounts.saveAndFlush(newAccount("ACC0000000002", BigDecimal.TEN));
        assertThatThrownBy(() -> accounts.saveAndFlush(newAccount("ACC0000000002", BigDecimal.TEN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * MySQL surfaces a CHECK violation with a different error code than a unique
     * violation, so Spring translates it to JpaSystemException rather than
     * DataIntegrityViolationException. Asserting on the constraint name keeps the
     * test about the guarantee rather than about the translation.
     */
    @Test
    void negativeBalanceIsRejectedByTheDatabase() {
        Account account = newAccount("ACC0000000003", new BigDecimal("-1.0000"));
        assertThatThrownBy(() -> accounts.saveAndFlush(account))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_accounts_balance_non_negative");
    }

    @Test
    void optimisticLockVersionStartsAtZero() {
        Account account = accounts.saveAndFlush(newAccount("ACC0000000004", BigDecimal.ZERO));
        assertThat(account.getVersion()).isZero();
    }

    /** The unique index is what makes a retried payment safe. */
    @Test
    void idempotencyKeyIsUniqueAcrossTransactions() {
        Account account = accounts.saveAndFlush(newAccount("ACC0000000005", new BigDecimal("500.0000")));

        transactions.saveAndFlush(newTransaction(account, "TXN-REF-1", "idem-key-1"));

        assertThatThrownBy(() ->
                        transactions.saveAndFlush(newTransaction(account, "TXN-REF-2", "idem-key-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void zeroAmountTransactionIsRejectedByTheDatabase() {
        Account account = accounts.saveAndFlush(newAccount("ACC0000000006", new BigDecimal("500.0000")));
        Transaction txn = newTransaction(account, "TXN-REF-3", "idem-key-3");
        txn.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> transactions.saveAndFlush(txn))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_txn_amount_positive");
    }

    @Test
    void statementQueryMatchesBothSidesOfATransfer() {
        Account from = accounts.saveAndFlush(newAccount("ACC0000000007", new BigDecimal("900.0000")));
        Account to = accounts.saveAndFlush(newAccount("ACC0000000008", new BigDecimal("100.0000")));

        Transaction transfer = newTransaction(from, "TXN-REF-4", "idem-key-4");
        transfer.setType(TransactionType.TRANSFER);
        transfer.setToAccount(to);
        transactions.saveAndFlush(transfer);

        assertThat(transactions.findByAccount(from.getId(), org.springframework.data.domain.Pageable.unpaged()))
                .hasSize(1);
        assertThat(transactions.findByAccount(to.getId(), org.springframework.data.domain.Pageable.unpaged()))
                .hasSize(1);
    }

    @Test
    void minimumBalanceRuleComesFromTheAccountType() {
        Account savings = newAccount("ACC0000000009", new BigDecimal("600.0000"));
        savings.setType(AccountType.SAVINGS);

        // SAVINGS keeps a 500 floor, so only 100 of the 600 is spendable.
        assertThat(savings.getAvailableBalance()).isEqualByComparingTo("100.0000");
        assertThat(savings.canDebit(new BigDecimal("100.00"))).isTrue();
        assertThat(savings.canDebit(new BigDecimal("150.00"))).isFalse();
    }

    @Test
    void blockedAccountRefusesDebitAndCredit() {
        Account account = newAccount("ACC0000000010", new BigDecimal("5000.0000"));
        account.setStatus(AccountStatus.BLOCKED);

        assertThat(account.canDebit(new BigDecimal("10.00"))).isFalse();
        assertThat(account.getStatus().allowsCredit()).isFalse();
    }

    /** Frozen means "stop money leaving" but incoming credits still land. */
    @Test
    void frozenAccountRefusesDebitButAllowsCredit() {
        Account account = newAccount("ACC0000000011", new BigDecimal("5000.0000"));
        account.setStatus(AccountStatus.FROZEN);

        assertThat(account.canDebit(new BigDecimal("10.00"))).isFalse();
        assertThat(account.getStatus().allowsCredit()).isTrue();
    }

    private User newUser(String email, String phone) {
        User user = new User();
        user.setFirstName("Asha");
        user.setLastName("Mehta");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash("$2a$10$placeholderplaceholderplaceholderplaceholderplaceholder");
        return user;
    }

    private Account newAccount(String accountNumber, BigDecimal balance) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUser(customer);
        account.setType(AccountType.CURRENT);
        account.setBalance(balance);
        return account;
    }

    private Transaction newTransaction(Account from, String reference, String idempotencyKey) {
        Transaction txn = new Transaction();
        txn.setReference(reference);
        txn.setIdempotencyKey(idempotencyKey);
        txn.setType(TransactionType.WITHDRAWAL);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(new BigDecimal("100.0000"));
        txn.setFromAccount(from);
        txn.setBalanceAfter(from.getBalance());
        return txn;
    }
}
