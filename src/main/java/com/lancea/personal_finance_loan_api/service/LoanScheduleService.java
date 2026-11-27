package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.aspect.Auditable;
import com.lancea.personal_finance_loan_api.dto.request.LoanPaymentRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanPaymentResponse;
import com.lancea.personal_finance_loan_api.dto.response.LoanResponse;
import com.lancea.personal_finance_loan_api.dto.response.LoanScheduleResponse;
import com.lancea.personal_finance_loan_api.dto.response.TransactionResponse;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.Loan;
import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import com.lancea.personal_finance_loan_api.entity.Transaction;
import com.lancea.personal_finance_loan_api.enums.Currency;
import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;
import com.lancea.personal_finance_loan_api.enums.LoanStatus;
import com.lancea.personal_finance_loan_api.enums.TransactionType;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.DuplicateTransactionException;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.LoanRepository;
import com.lancea.personal_finance_loan_api.repository.LoanScheduleRepository;
import com.lancea.personal_finance_loan_api.repository.TransactionRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanScheduleService {

    private final LoanScheduleRepository loanScheduleRepository;
    private final LoanRepository loanRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final LoanScheduleCache loanScheduleCache;
    private final CacheManager cacheManager;

    @PersistenceContext
    private EntityManager entityManager;

    public List<LoanScheduleResponse> getLoanSchedules(UUID loanId, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        if(!loanRepository.existsByIdAndUserIdAndIsDeletedFalse(loanId, userId))
            throw new ResourceNotFoundException("Loan doesn't exist");

        return loanScheduleCache.getSchedules(loanId);
    }

    public LoanScheduleResponse getSpecificLoanSchedule(UUID loanId, int paymentNumber, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        if(!loanRepository.existsByIdAndUserIdAndIsDeletedFalse(loanId, userId))
            throw new ResourceNotFoundException("Loan doesn't exist");

        LoanSchedule loanSchedule = loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, paymentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment schedule not found"));

        return LoanScheduleResponse.of(loanSchedule);
    }

    @CacheEvict(value = "loan-schedule", key = "#loanId")
    @Auditable(action = "LOAN_PAYMENT", entityType = "LOAN_SCHEDULE")
    @Transactional
    public LoanPaymentResponse payInstallment(UUID loanId, int paymentNumber,
                               LoanPaymentRequest loanPaymentRequest, Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        UUID accountId = loanRepository.findFundingAccountId(loanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan doesn't exist"));
        Account account = accountRepository.findForUpdateByIdAndUserIdAndIsDeletedFalse(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Funding account doesn't exist or has been deleted"));

        // Load payment state after acquiring the lock so a waiting payment sees the committed state.
        Loan loan = loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan doesn't exist"));

        Optional<Transaction> existingTransaction = transactionRepository
                .findByIdempotencyKeyAndAccountIdAndAccountUserId(
                        loanPaymentRequest.idempotencyKey(), accountId, userId);

        if (existingTransaction.isPresent()) {
            Transaction payment = existingTransaction.get();
            LoanSchedule paidSchedule = payment.getLoanSchedule();
            if (payment.getType() != TransactionType.LOAN_PAYMENT
                    || payment.getLoan() == null
                    || !loanId.equals(payment.getLoan().getId())
                    || paidSchedule == null
                    || !loanId.equals(paidSchedule.getLoan().getId())
                    || paidSchedule.getPaymentNumber() != paymentNumber
                    || payment.getLoanStatusAfterPayment() == null) {
                throw new DuplicateTransactionException("Idempotency key already used for a different or unverifiable request.");
            }
            return generateLoanPaymentResponse(loan, paidSchedule, payment);
        }

        Currency.requireSupported(account.getCurrency());
        LoanSchedule loanSchedule = loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, paymentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment schedule not found"));

        checkIfLoanIsPaidOff(loan);
        checkIfLoanScheduleIsAlreadyPaid(loanSchedule);
        checkForPreviousTransaction(loanId, paymentNumber);

        validateAccountEligibleForTransaction(account, loanSchedule.getPaymentAmount());

        account.setBalance(account.getBalance()
                .subtract(loanSchedule.getPaymentAmount()));

        loanSchedule.setStatus(LoanScheduleStatus.PAID);

        if (!loanScheduleRepository.existsByLoanIdAndPaymentNumberGreaterThanAndStatus(
                loanId, paymentNumber, LoanScheduleStatus.PENDING)) {
            loan.setStatus(LoanStatus.PAID_OFF);
        }

        Transaction transactionCopyOfThePayment = Transaction.builder()
                .account(account)
                .loan(loan)
                .loanSchedule(loanSchedule)
                .loanStatusAfterPayment(loan.getStatus())
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.LOAN_PAYMENT)
                .amount(loanSchedule.getPaymentAmount())
                .category("LOAN_REPAYMENT")
                .idempotencyKey(loanPaymentRequest.idempotencyKey())
                .transactedAt(Instant.now())
                .build();


        loanScheduleRepository.save(loanSchedule);
        loanRepository.save(loan);
        accountRepository.save(account);
        Transaction savedPayment = transactionRepository.saveAndFlush(transactionCopyOfThePayment);
        entityManager.refresh(savedPayment);

        LocalDate date = savedPayment.getTransactedAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        String cacheKey = userId + ":" + date.getYear() + ":" + date.getMonthValue();

        Cache cache = cacheManager.getCache("spending-summary");

        if (cache != null){
            cache.evict(cacheKey);
        }

        return generateLoanPaymentResponse(loan, loanSchedule, savedPayment);
    }

    private void checkIfLoanIsPaidOff(Loan loan){
        if(loan.getStatus() == LoanStatus.PAID_OFF) throw new BadRequestException("Loan is paid off");
    }

    private void checkIfLoanScheduleIsAlreadyPaid(LoanSchedule loanSchedule){
        if(loanSchedule.getStatus() == LoanScheduleStatus.PAID) throw new BadRequestException("This schedule has already been paid");
    }

    private void checkForPreviousTransaction(UUID loanId, int paymentNumber){
        boolean hasPendingPreviousPayments = loanScheduleRepository
                .existsByLoanIdAndPaymentNumberLessThanAndStatus(loanId, paymentNumber, LoanScheduleStatus.PENDING);

        if(hasPendingPreviousPayments) throw new BadRequestException("Previous installment must be paid before this one");
    }

    private void validateAccountEligibleForTransaction(Account account, BigDecimal amount){

        if(!account.getIsActive())  throw new BadRequestException("Account must be active");

        if(account.getBalance().compareTo(amount) < 0) throw new BadRequestException("Account doesn't have enough balance");

    }

    private LoanPaymentResponse generateLoanPaymentResponse (Loan loan, LoanSchedule loanSchedule, Transaction transaction){

        return new LoanPaymentResponse(LoanResponse.of(loan, transaction.getLoanStatusAfterPayment()),
                LoanScheduleResponse.of(loanSchedule),
                TransactionResponse.of(transaction));
    }
}
