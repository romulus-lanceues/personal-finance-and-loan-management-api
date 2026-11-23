package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.DepositRequest;
import com.lancea.personal_finance_loan_api.dto.request.LoanPaymentRequest;
import com.lancea.personal_finance_loan_api.dto.request.TransferRequest;
import com.lancea.personal_finance_loan_api.dto.request.WithdrawRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanPaymentResponse;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;
import com.lancea.personal_finance_loan_api.enums.LoanStatus;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.DuplicateTransactionException;
import com.lancea.personal_finance_loan_api.exception.GlobalExceptionHandler;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = TransactionIdempotencyIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE ,
        properties = {"spring.jpa.show-sql=false", "spring.flyway.baseline-on-migrate=false",
                "logging.level.com.lancea.personal_finance_loan_api.exception.GlobalExceptionHandler=OFF"})
@EnabledIf(value = "hasDatabaseConfiguration",
        disabledReason = "Set idempotency.test.jdbcUrl or IDEMPOTENCY_TEST_JDBC_URL to run against local PostgreSQL.")
class TransactionIdempotencyIntegrationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = DataRedisAutoConfiguration.class)
    @EnableJpaAuditing
    @EnableJpaRepositories(basePackageClasses = AccountRepository.class)
    @EntityScan(basePackageClasses = Account.class)
    @Import({TransactionService.class, LoanScheduleService.class, LoanScheduleCache.class,
            ReferenceNumberGenerator.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("loan-schedule", "spending-summary");
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = configuredJdbcUrl();
        if (!url.matches("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):[0-9]+/idempotency_test")) {
            throw new IllegalStateException(
                    "Use a PostgreSQL URL on localhost with the database name idempotency_test.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getProperty("idempotency.test.user",
                System.getenv().getOrDefault("IDEMPOTENCY_TEST_DB_USER", "postgres")));
        registry.add("spring.datasource.password",
                () -> System.getenv().getOrDefault("IDEMPOTENCY_TEST_DB_PASSWORD", ""));
    }

    static boolean hasDatabaseConfiguration() {
        return !configuredJdbcUrl().isBlank();
    }

    private static String configuredJdbcUrl() {
        return System.getProperty("idempotency.test.jdbcUrl",
                System.getenv().getOrDefault("IDEMPOTENCY_TEST_JDBC_URL", ""));
    }

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TransactionService transactions;
    @Autowired
    private LoanScheduleService installments;
    @Autowired
    private GlobalExceptionHandler exceptionHandler;
    @Autowired
    private Validator validator;

    @Test
    void concurrentInstallmentRetriesWaitForTheAccountAndReplayOnePayment() throws Exception {
        UUID owner = createUser();
        UUID account = createAccount(owner);
        UUID loan = createLoan(owner, account);
        Jwt jwt = jwt(owner);
        LoanPaymentRequest request = new LoanPaymentRequest("concurrent");

        LoanPaymentResponse paid = assertConcurrentRetry(account, () -> installments.payInstallment(loan, 1, request, jwt));
        assertThat(paid.loanResponse().status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(balance(account)).isEqualByComparingTo("75.00");
        assertThat(transactionCount(account)).isEqualTo(1);

        assertThatThrownBy(() -> installments.payInstallment(loan, 1, new LoanPaymentRequest("another"), jwt))
                .isInstanceOf(BadRequestException.class);
        assertThat(installments.payInstallment(loan, 2, new LoanPaymentRequest("final"), jwt)
                .loanResponse().status()).isEqualTo(LoanStatus.PAID_OFF);
        assertThat(installments.payInstallment(loan, 1, request, jwt)).isEqualTo(paid);
        assertThat(balance(account)).isEqualByComparingTo("50.00");
        assertThat(transactionCount(account)).isEqualTo(2);
    }

    @Test
    void concurrentRetriesForDepositsWithdrawalsAndTransfersEachMoveMoneyOnce() throws Exception {
        UUID owner = createUser();
        UUID account = createAccount(owner);
        UUID destination = createAccount(owner);
        Jwt jwt = jwt(owner);
        BigDecimal amount = new BigDecimal("12.50");

        var deposit = new DepositRequest(account, amount, "Salary", null, "concurrent-deposit");
        assertConcurrentRetry(account, () -> transactions.deposit(deposit, jwt));
        assertThat(balance(account)).isEqualByComparingTo("112.50");
        assertThat(transactionCount(account)).isEqualTo(1);

        var withdrawal = new WithdrawRequest(account, amount, "Goods", null, "concurrent-withdrawal");
        assertConcurrentRetry(account, () -> transactions.withdraw(withdrawal, jwt));
        assertThat(balance(account)).isEqualByComparingTo("100.00");
        assertThat(transactionCount(account)).isEqualTo(2);

        var transfer = new TransferRequest(account, destination, amount, null, "concurrent-transfer");
        assertThat(assertConcurrentRetry(account, () -> transactions.transfer(transfer, jwt))).hasSize(2);
        assertThat(balance(account)).isEqualByComparingTo("87.50");
        assertThat(balance(destination)).isEqualByComparingTo("112.50");
        assertThat(transactionCount(account)).isEqualTo(3);
        assertThat(transactionCount(destination)).isEqualTo(1);
    }

    @Test
    void concurrentWithdrawalsWithDifferentKeysCannotBothOverdrawTheAccount() throws Exception {
        UUID owner = createUser();
        UUID account = createAccount(owner);
        Jwt jwt = jwt(owner);
        BigDecimal amount = new BigDecimal("80.00");
        var first = new WithdrawRequest(account, amount, "Goods", null, "withdrawal-1");
        var second = new WithdrawRequest(account, amount, "Goods", null, "withdrawal-2");

        var rejections = runConcurrently(account,
                () -> badRequestMessage(() -> transactions.withdraw(first, jwt)),
                () -> badRequestMessage(() -> transactions.withdraw(second, jwt)));

        assertThat(rejections).containsExactlyInAnyOrder(Optional.empty(), Optional.of("Insufficient balance"));
        assertThat(balance(account)).isEqualByComparingTo("20.00");
        assertThat(transactionCount(account)).isEqualTo(1);
    }

    @Test
    void concurrentDepositsWithDifferentKeysBothIncreaseTheBalance() throws Exception {
        UUID owner = createUser();
        UUID account = createAccount(owner);
        Jwt jwt = jwt(owner);
        var first = new DepositRequest(account, new BigDecimal("12.50"), "Salary", null, "deposit-1");
        var second = new DepositRequest(account, new BigDecimal("7.25"), "Salary", null, "deposit-2");

        runConcurrently(account, () -> transactions.deposit(first, jwt), () -> transactions.deposit(second, jwt));

        assertThat(balance(account)).isEqualByComparingTo("119.75");
        assertThat(transactionCount(account)).isEqualTo(2);
    }

    @Test
    void concurrentTransfersInOppositeDirectionsPreserveBothBalanceChanges() throws Exception {
        UUID owner = createUser();
        UUID firstAccount = createAccount(owner);
        UUID secondAccount = createAccount(owner);
        Jwt jwt = jwt(owner);
        var outbound = new TransferRequest(firstAccount, secondAccount, new BigDecimal("30.00"), null, "outbound");
        var inbound = new TransferRequest(secondAccount, firstAccount, new BigDecimal("15.00"), null, "inbound");
        UUID firstLockedAccount = firstAccount.compareTo(secondAccount) < 0 ? firstAccount : secondAccount;

        runConcurrently(firstLockedAccount,
                () -> transactions.transfer(outbound, jwt), () -> transactions.transfer(inbound, jwt));

        assertThat(balance(firstAccount)).isEqualByComparingTo("85.00");
        assertThat(balance(secondAccount)).isEqualByComparingTo("115.00");
        assertThat(transactionCount(firstAccount)).isEqualTo(2);
        assertThat(transactionCount(secondAccount)).isEqualTo(2);
    }

    @Test
    void concurrentPaymentsWithDifferentKeysPayTheFinalInstallmentOnlyOnce() throws Exception {
        UUID owner = createUser();
        UUID account = createAccount(owner);
        UUID loan = createLoan(owner, account);
        Jwt jwt = jwt(owner);
        installments.payInstallment(loan, 1, new LoanPaymentRequest("first-installment"), jwt);
        var first = new LoanPaymentRequest("final-payment-1");
        var second = new LoanPaymentRequest("final-payment-2");

        var rejections = runConcurrently(account,
                () -> badRequestMessage(() -> installments.payInstallment(loan, 2, first, jwt)),
                () -> badRequestMessage(() -> installments.payInstallment(loan, 2, second, jwt)));

        assertThat(rejections).containsExactlyInAnyOrder(Optional.empty(), Optional.of("Loan is paid off"));
        assertThat(balance(account)).isEqualByComparingTo("50.00");
        assertThat(transactionCount(account)).isEqualTo(2);
        assertThat(installments.getSpecificLoanSchedule(loan, 2, jwt).loanScheduleStatus())
                .isEqualTo(LoanScheduleStatus.PAID);
        assertThat(jdbc.queryForObject("select status from loans where id = ?", String.class, loan))
                .isEqualTo(LoanStatus.PAID_OFF.name());
    }

    @Test
    void rejectsMismatchesAndLegacyPaymentsAndLetsOtherAccountsReuseTheKey() {
        UUID owner = createUser();
        UUID otherOwner = createUser();
        UUID account = createAccount(owner);
        UUID otherAccount = createAccount(otherOwner);
        UUID loan = createLoan(owner, account);
        UUID anotherLoan = createLoan(owner, account);
        UUID otherLoan = createLoan(otherOwner, otherAccount);
        Jwt jwt = jwt(owner);
        LoanPaymentRequest request = new LoanPaymentRequest("shared");

        var first = installments.payInstallment(loan, 1, request, jwt);
        assertConflict(() -> installments.payInstallment(loan, 2, request, jwt));
        assertConflict(() -> installments.payInstallment(anotherLoan, 1, request, jwt));
        assertConflict(() -> transactions.deposit(
                new DepositRequest(account, new BigDecimal("25"), "Payment", null, "shared"), jwt));
        assertThatThrownBy(() -> installments.payInstallment(loan, 1, request, jwt(otherOwner)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(installments.payInstallment(otherLoan, 1, request, jwt(otherOwner)).transactionResponse().id())
                .isNotEqualTo(first.transactionResponse().id());

        jdbc.update("update transactions set loan_schedule_id = null, loan_status_after_payment = null where id = ?",
                first.transactionResponse().id());
        assertConflict(() -> installments.payInstallment(loan, 1, request, jwt));
        assertThat(transactionCount(account)).isEqualTo(1);
        assertThat(balance(account)).isEqualByComparingTo("75.00");
        assertThat(balance(otherAccount)).isEqualByComparingTo("75.00");
    }

    @Test
    void replaysDepositsWithdrawalsAndBothTransferEntriesWithTheSameResponse() {
        UUID owner = createUser();
        UUID account = createAccount(owner);
        UUID destination = createAccount(owner);
        UUID differentDestination = createAccount(owner);
        Jwt jwt = jwt(owner);
        BigDecimal amount = new BigDecimal("12.50");

        var deposit = new DepositRequest(account, amount, "Salary", null, "deposit");
        var withdrawal = new WithdrawRequest(account, amount, "Goods", "Books", "withdrawal");
        var transfer = new TransferRequest(account, destination, amount, "Savings", "transfer");

        var deposited = transactions.deposit(deposit, jwt);
        var withdrawn = transactions.withdraw(withdrawal, jwt);
        var transferred = transactions.transfer(transfer, jwt);
        assertThat(transferred.getFirst().transactedAt()).isEqualTo(transferred.getLast().transactedAt());
        assertThat(transactions.deposit(deposit, jwt)).isEqualTo(deposited);
        assertThat(transactions.withdraw(withdrawal, jwt)).isEqualTo(withdrawn);
        assertThat(transactions.transfer(transfer, jwt)).containsExactlyElementsOf(transferred);
        assertConflict(() -> transactions.deposit(
                new DepositRequest(account, new BigDecimal("15.00"), "Salary", null, "deposit"), jwt));
        assertConflict(() -> transactions.deposit(
                new DepositRequest(account, amount, "Other", null, "deposit"), jwt));
        assertConflict(() -> transactions.withdraw(
                new WithdrawRequest(account, amount, "Goods", "Different purpose", "withdrawal"), jwt));
        assertConflict(() -> transactions.transfer(
                new TransferRequest(account, differentDestination, amount, "Savings", "transfer"), jwt));
        Jwt outsider = jwt(createUser());
        assertThatThrownBy(() -> transactions.deposit(deposit, outsider))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> transactions.withdraw(withdrawal, outsider))
                .isInstanceOf(ResourceNotFoundException.class);
        UUID outsiderAccount = createAccount(UUID.fromString(outsider.getClaimAsString("userId")));
        assertThatThrownBy(() -> transactions.transfer(
                new TransferRequest(account, outsiderAccount, amount, "Savings", "transfer"), jwt))
                .isInstanceOf(ResourceNotFoundException.class);
        var independentDeposit = transactions.deposit(
                new DepositRequest(differentDestination, amount, "Salary", null, "deposit"), jwt);
        assertThat(independentDeposit.id()).isNotEqualTo(deposited.id());
        assertThat(balance(account)).isEqualByComparingTo("87.50");
        assertThat(balance(destination)).isEqualByComparingTo("112.50");
        assertThat(balance(differentDestination)).isEqualByComparingTo("112.50");
        assertThat(transactionCount(account)).isEqualTo(3);
        assertThat(transactionCount(destination)).isEqualTo(1);
    }

    @Test
    void failedTransactionWriteRollsBackTheBalanceAndLeavesTheKeyAvailable() {
        UUID owner = createUser();
        UUID account = createAccount(owner);
        Jwt jwt = jwt(owner);
        BigDecimal amount = new BigDecimal("5.00");

        assertThatThrownBy(() -> transactions.deposit(
                new DepositRequest(account, amount, "Salary", "a".repeat(256), "failed-write"), jwt))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(balance(account)).isEqualByComparingTo("100.00");
        assertThat(transactionCount(account)).isZero();

        transactions.deposit(new DepositRequest(account, amount, "Salary", null, "failed-write"), jwt);
        assertThat(balance(account)).isEqualByComparingTo("105.00");
        assertThat(transactionCount(account)).isEqualTo(1);
    }

    @Test
    void monthlySummaryUsesUtcMonthBoundariesInManilaSession() {
        UUID owner = createUser();
        UUID account = createAccount(owner);
        Jwt jwt = jwt(owner);
        var novemberDeposit = transactions.deposit(
                new DepositRequest(account, new BigDecimal("12.50"), "Salary", null, "november"), jwt);
        var decemberDeposit = transactions.deposit(
                new DepositRequest(account, new BigDecimal("7.25"), "Salary", null, "december"), jwt);
        jdbc.update("update transactions set transacted_at = ? where id = ?",
                OffsetDateTime.parse("2026-11-30T18:00:00Z"), novemberDeposit.id());
        jdbc.update("update transactions set transacted_at = ? where id = ?",
                OffsetDateTime.parse("2026-12-01T00:00:00Z"), decemberDeposit.id());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.execute("set local time zone 'Asia/Manila'");

            assertThat(transactions.getMonthlySummary(2026, 11, jwt)).singleElement().satisfies(summary -> {
                assertThat(summary.totalAmount()).isEqualByComparingTo("12.50");
                assertThat(summary.transactionCount()).isEqualTo(1L);
            });
            assertThat(transactions.getMonthlySummary(2026, 12, jwt)).singleElement().satisfies(summary -> {
                assertThat(summary.totalAmount()).isEqualByComparingTo("7.25");
                assertThat(summary.transactionCount()).isEqualTo(1L);
            });
        });
    }

    @Test
    void requestValidationRejectsKeysThatExceedTheDatabaseLimit() {
        UUID account = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        String supplementaryCharacter = "\uD83D\uDE00";
        for (String key : List.of("a".repeat(255), "a".repeat(256),
                supplementaryCharacter.repeat(255), supplementaryCharacter.repeat(256))) {
            List<Object> requests = List.of(
                    new DepositRequest(account, amount, "Salary", null, key),
                    new WithdrawRequest(account, amount, "Goods", null, key),
                    new TransferRequest(account, destination, amount, null, key),
                    new LoanPaymentRequest(key));
            for (Object request : requests) {
                var violations = validator.validate(request);
                if (key.codePointCount(0, key.length()) == 255) {
                    assertThat(violations).isEmpty();
                } else {
                    assertThat(violations).singleElement()
                            .satisfies(violation -> assertThat(violation.getPropertyPath().toString())
                                    .isEqualTo("idempotencyKey"));
                }
            }
        }
    }

    private <T> T assertConcurrentRetry(UUID account, Callable<T> request) throws Exception {
        List<T> results = runConcurrently(account, request, request);
        assertThat(results.getLast()).isEqualTo(results.getFirst());
        return results.getFirst();
    }

    private <T> List<T> runConcurrently(UUID account, Callable<T> firstRequest, Callable<T> secondRequest)
            throws Exception {
        try (var executor = Executors.newFixedThreadPool(2);
             var blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (var lock = blocker.prepareStatement("select id from accounts where id = ? for update")) {
                lock.setObject(1, account);
                lock.executeQuery().close();
            }

            var first = executor.submit(firstRequest);
            var second = executor.submit(secondRequest);
            try {
                awaitTwoAccountLockWaiters();
            } finally {
                blocker.rollback();
            }

            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
    }

    private Optional<String> badRequestMessage(Runnable request) {
        try {
            request.run();
            return Optional.empty();
        } catch (BadRequestException exception) {
            return Optional.of(exception.getMessage());
        }
    }

    private void awaitTwoAccountLockWaiters() throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            Long waiters = jdbc.queryForObject("""
                    select count(*) from pg_stat_activity
                    where datname = current_database() and wait_event_type = 'Lock'
                      and lower(query) like '%for%update%'
                    """, Long.class);
            if (waiters != null && waiters == 2) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Expected both requests to wait for the account write lock");
    }

    private void assertConflict(Runnable request) {
        var exception = assertThrows(DuplicateTransactionException.class, request::run);
        assertThat(exceptionHandler.handleDuplicateTransactions(exception).getStatus())
                .isEqualTo(HttpStatus.CONFLICT.value());
    }

    private UUID createUser() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into users (id, email, full_name, password, provider, is_deleted, created_at, updated_at)
                values (?, ?, 'Test User', 'unused', 'LOCAL', false, now(), now())
                """, id, id + "@example.invalid");
        return id;
    }

    private UUID createAccount(UUID owner) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into accounts (id, user_id, account_name, account_type, currency, balance,
                                      is_active, is_deleted, created_at, updated_at)
                values (?, ?, 'Test Account', 'CHECKING', 'PHP', 100, true, false, now(), now())
                """, id, owner);
        return id;
    }

    private UUID createLoan(UUID owner, UUID account) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into loans (id, user_id, account_id, loan_name, principal, annual_rate,
                                   monthly_payment, term_months, status, is_deleted, disbursed_at,
                                   maturity_date, created_at, updated_at)
                values (?, ?, ?, 'Test Loan', 50, 0, 25, 2, 'ACTIVE', false,
                        current_date, current_date + 60, now(), now())
                """, id, owner, account);
        for (int number : List.of(1, 2)) {
            jdbc.update("""
                    insert into loan_schedules (id, loan_id, payment_number, payment_amount, principal_portion,
                                                interest_portion, remaining_balance, due_date, status,
                                                created_at, updated_at)
                    values (?, ?, ?, 25, 25, 0, ?, current_date + 30, 'PENDING', now(), now())
                    """, UUID.randomUUID(), id, number, 50 - 25 * number);
        }
        return id;
    }

    private BigDecimal balance(UUID account) {
        return jdbc.queryForObject("select balance from accounts where id = ?", BigDecimal.class, account);
    }

    private long transactionCount(UUID account) {
        return jdbc.queryForObject("select count(*) from transactions where account_id = ?", Long.class, account);
    }

    private Jwt jwt(UUID owner) {
        return Jwt.withTokenValue("test").header("alg", "none").claim("userId", owner.toString()).build();
    }
}
