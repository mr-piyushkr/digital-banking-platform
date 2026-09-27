package com.bankflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.bankflow.dto.request.BeneficiaryRequest;
import com.bankflow.dto.request.DepositRequest;
import com.bankflow.dto.request.OpenAccountRequest;
import com.bankflow.dto.request.RegisterRequest;
import com.bankflow.dto.request.TransferRequest;
import com.bankflow.dto.request.WithdrawRequest;
import com.bankflow.dto.response.AccountResponse;
import com.bankflow.dto.response.BeneficiaryResponse;
import com.bankflow.dto.response.TransactionResponse;
import com.bankflow.entity.enums.AccountStatus;
import com.bankflow.entity.enums.AccountType;
import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.repository.AccountRepository;
import com.bankflow.support.TestFlywayConfig;

/**
 * The tests that matter most in a banking application, run against real MySQL
 * because they are about InnoDB behaviour: row locking, unique constraints and
 * transaction boundaries.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestFlywayConfig.class)
class MoneyMovementIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;
    @Autowired private TransferService transferService;
    @Autowired private BeneficiaryService beneficiaryService;
    @Autowired private AccountRepository accountRepository;

    private Long ashaId;
    private Long ravinId;
    private AccountResponse ashaAccount;
    private AccountResponse ravinAccount;

    @BeforeEach
    void setUp() {
        ashaId = register("asha", null).id();
        ravinId = register("ravin", null).id();

        ashaAccount = accountService.openAccount(
                ashaId, new OpenAccountRequest(AccountType.CURRENT, new BigDecimal("10000.00")));
        ravinAccount = accountService.openAccount(
                ravinId, new OpenAccountRequest(AccountType.CURRENT, new BigDecimal("2000.00")));
    }

    // ---------- deposit ----------

    @Test
    void depositIncreasesBalanceAndRecordsBalanceAfter() {
        TransactionResponse txn = transactionService.deposit(
                ashaId, new DepositRequest(ashaAccount.accountNumber(), new BigDecimal("500.00"), "salary"),
                "dep-1");

        assertThat(txn.status()).isEqualTo("SUCCESS");
        assertThat(txn.direction()).isEqualTo("CREDIT");
        assertThat(txn.balanceAfter()).isEqualByComparingTo("10500.0000");
        assertThat(balanceOf(ashaAccount)).isEqualByComparingTo("10500.0000");
    }

    /** The core idempotency guarantee: a retried request must not charge twice. */
    @Test
    void replayingTheSameIdempotencyKeyDoesNotMoveMoneyAgain() {
        DepositRequest request =
                new DepositRequest(ashaAccount.accountNumber(), new BigDecimal("500.00"), "salary");

        TransactionResponse first = transactionService.deposit(ashaId, request, "same-key");
        TransactionResponse second = transactionService.deposit(ashaId, request, "same-key");

        assertThat(second.reference()).isEqualTo(first.reference());
        assertThat(balanceOf(ashaAccount)).isEqualByComparingTo("10500.0000");
    }

    @Test
    void depositIntoAnotherCustomersAccountIsNotFound() {
        assertThatThrownBy(() -> transactionService.deposit(
                        ashaId,
                        new DepositRequest(ravinAccount.accountNumber(), new BigDecimal("100.00"), null),
                        "dep-x"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ---------- withdraw ----------

    @Test
    void withdrawalReducesBalance() {
        transactionService.withdraw(
                ashaId, new WithdrawRequest(ashaAccount.accountNumber(), new BigDecimal("2500.00"), null),
                "wd-1");

        assertThat(balanceOf(ashaAccount)).isEqualByComparingTo("7500.0000");
    }

    @Test
    void withdrawalBeyondBalanceIsRejected() {
        assertThatThrownBy(() -> transactionService.withdraw(
                        ashaId,
                        new WithdrawRequest(ashaAccount.accountNumber(), new BigDecimal("99999.00"), null),
                        "wd-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        assertThat(balanceOf(ashaAccount)).isEqualByComparingTo("10000.0000");
    }

    @Test
    void savingsAccountCannotBeDrawnBelowItsMinimumBalance() {
        AccountResponse savings = accountService.openAccount(
                ashaId, new OpenAccountRequest(AccountType.SAVINGS, new BigDecimal("1000.00")));

        // 500 of the 1000 is the SAVINGS floor, so only 500 is spendable.
        assertThatThrownBy(() -> transactionService.withdraw(
                        ashaId,
                        new WithdrawRequest(savings.accountNumber(), new BigDecimal("600.00"), null),
                        "wd-min"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    void blockedAccountCannotBeDebited() {
        accountService.changeStatus(ashaAccount.id(), AccountStatus.BLOCKED, "fraud review");

        assertThatThrownBy(() -> transactionService.withdraw(
                        ashaId,
                        new WithdrawRequest(ashaAccount.accountNumber(), new BigDecimal("100.00"), null),
                        "wd-blocked"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.ACCOUNT_BLOCKED);
    }

    // ---------- transfer ----------

    @Test
    void transferMovesMoneyBetweenAccountsInOneTransaction() {
        verifiedBeneficiary(ashaId, ravinAccount.accountNumber(), "Ravin");

        TransactionResponse txn = transferService.transfer(
                ashaId,
                new TransferRequest(
                        ashaAccount.accountNumber(), ravinAccount.accountNumber(),
                        new BigDecimal("1500.00"), "rent"),
                "tr-1");

        assertThat(txn.type()).isEqualTo("TRANSFER");
        assertThat(txn.direction()).isEqualTo("DEBIT");
        assertThat(balanceOf(ashaAccount)).isEqualByComparingTo("8500.0000");
        assertThat(balanceOf(ravinAccount)).isEqualByComparingTo("3500.0000");
    }

    @Test
    void transferToAnUnverifiedBeneficiaryIsRejected() {
        beneficiaryService.add(
                ashaId,
                new BeneficiaryRequest(ravinAccount.accountNumber(), "Ravin", null, null));

        assertThatThrownBy(() -> transferService.transfer(
                        ashaId,
                        new TransferRequest(
                                ashaAccount.accountNumber(), ravinAccount.accountNumber(),
                                new BigDecimal("100.00"), null),
                        "tr-unverified"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.BENEFICIARY_NOT_VERIFIED);
    }

    @Test
    void transferWithoutAnyBeneficiaryIsRejected() {
        assertThatThrownBy(() -> transferService.transfer(
                        ashaId,
                        new TransferRequest(
                                ashaAccount.accountNumber(), ravinAccount.accountNumber(),
                                new BigDecimal("100.00"), null),
                        "tr-none"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.BENEFICIARY_NOT_VERIFIED);
    }

    @Test
    void transferToTheSameAccountIsRejected() {
        assertThatThrownBy(() -> transferService.transfer(
                        ashaId,
                        new TransferRequest(
                                ashaAccount.accountNumber(), ashaAccount.accountNumber(),
                                new BigDecimal("100.00"), null),
                        "tr-self"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.SAME_ACCOUNT_TRANSFER);
    }

    /** Moving between your own accounts needs no beneficiary. */
    @Test
    void transferBetweenOwnAccountsSkipsTheBeneficiaryCheck() {
        AccountResponse second = accountService.openAccount(
                ashaId, new OpenAccountRequest(AccountType.CURRENT, BigDecimal.ZERO));

        transferService.transfer(
                ashaId,
                new TransferRequest(
                        ashaAccount.accountNumber(), second.accountNumber(),
                        new BigDecimal("4000.00"), "savings top-up"),
                "tr-own");

        assertThat(balanceOf(ashaAccount)).isEqualByComparingTo("6000.0000");
        assertThat(balanceOf(second)).isEqualByComparingTo("4000.0000");
    }

    @Test
    void insufficientBalanceLeavesBothAccountsUntouched() {
        verifiedBeneficiary(ashaId, ravinAccount.accountNumber(), "Ravin");

        assertThatThrownBy(() -> transferService.transfer(
                        ashaId,
                        new TransferRequest(
                                ashaAccount.accountNumber(), ravinAccount.accountNumber(),
                                new BigDecimal("50000.00"), null),
                        "tr-poor"))
                .isInstanceOf(BusinessException.class);

        assertThat(balanceOf(ashaAccount)).isEqualByComparingTo("10000.0000");
        assertThat(balanceOf(ravinAccount)).isEqualByComparingTo("2000.0000");
    }

    // ---------- concurrency ----------

    /**
     * Twelve threads each try to withdraw 1000 from an account holding 10000, so
     * at most ten can succeed.
     *
     * The assertion is the invariant rather than an exact success count: a
     * thread may also legitimately fail on an InnoDB lock-wait timeout under
     * contention, which is a refusal, not a lost update. What must always hold
     * is that the balance equals the opening balance minus exactly the
     * withdrawals that reported success, and never drops below zero. Without
     * SELECT ... FOR UPDATE, threads would read the same stale balance and the
     * two sides of that equation would diverge.
     */
    @Test
    void parallelWithdrawalsCanNeverOverdrawTheAccount() throws Exception {
        int threads = 12;
        BigDecimal each = new BigDecimal("1000.00");

        List<Callable<Boolean>> jobs = IntStream.range(0, threads)
                .<Callable<Boolean>>mapToObj(i -> () -> {
                    try {
                        transactionService.withdraw(
                                ashaId,
                                new WithdrawRequest(ashaAccount.accountNumber(), each, null),
                                "concurrent-" + i);
                        return true;
                    } catch (RuntimeException e) {
                        return false;
                    }
                })
                .toList();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long succeeded;
        try {
            List<Future<Boolean>> results = pool.invokeAll(jobs);
            succeeded = results.stream().filter(MoneyMovementIntegrationTest::resolve).count();
        } finally {
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        BigDecimal expected = new BigDecimal("10000.00")
                .subtract(each.multiply(BigDecimal.valueOf(succeeded)));

        assertThat(succeeded).isPositive().isLessThanOrEqualTo(10);
        assertThat(balanceOf(ashaAccount)).isEqualByComparingTo(expected);
        assertThat(balanceOf(ashaAccount)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    // ---------- helpers ----------

    private static boolean resolve(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * These tests commit for real — rollback would defeat the concurrency test,
     * which needs other threads to see the rows. So each test gets its own
     * identities instead of a shared fixture that would collide on the unique
     * email and phone constraints.
     */
    private static final java.util.concurrent.atomic.AtomicInteger SEQUENCE =
            new java.util.concurrent.atomic.AtomicInteger();

    private com.bankflow.dto.response.UserResponse register(String handle, String unusedPhone) {
        int n = SEQUENCE.incrementAndGet();
        String phone = "9" + String.format("%09d", n);
        return authService
                .register(new RegisterRequest(
                        handle, "Test", handle + n + "@example.com", phone,
                        "Str0ngPassword", null, null, null, null))
                .user();
    }

    private BeneficiaryResponse verifiedBeneficiary(Long ownerId, String accountNumber, String name) {
        BeneficiaryResponse added = beneficiaryService.add(
                ownerId, new BeneficiaryRequest(accountNumber, name, null, null));
        return beneficiaryService.verify(ownerId, added.id());
    }

    private BigDecimal balanceOf(AccountResponse account) {
        return accountRepository.findById(account.id()).orElseThrow().getBalance();
    }
}
