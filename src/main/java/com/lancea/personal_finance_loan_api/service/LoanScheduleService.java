package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.LoanPaymentRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanPaymentResponse;
import com.lancea.personal_finance_loan_api.dto.response.LoanResponse;
import com.lancea.personal_finance_loan_api.dto.response.LoanScheduleResponse;
import com.lancea.personal_finance_loan_api.dto.response.TransactionResponse;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.Loan;
import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import com.lancea.personal_finance_loan_api.entity.Transaction;
import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;
import com.lancea.personal_finance_loan_api.enums.LoanStatus;
import com.lancea.personal_finance_loan_api.enums.TransactionType;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.LoanRepository;
import com.lancea.personal_finance_loan_api.repository.LoanScheduleRepository;
import com.lancea.personal_finance_loan_api.repository.TransactionRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public List<LoanScheduleResponse> getLoanSchedules(UUID loanId, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        if(!loanRepository.existsByIdAndUserIdAndIsDeletedFalse(loanId, userId))
            throw new ResourceNotFoundException("Loan doesn't exist");

        //Retrieve the LoanSchedules and convert them into LoanScheduleResponses
        return loanScheduleRepository
                .findByLoanId(loanId)
                .stream()
                .map(LoanScheduleResponse::of)
                .toList();
    }

    public LoanScheduleResponse getSpecificLoanSchedule(UUID loanId, int paymentNumber, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        if(!loanRepository.existsByIdAndUserIdAndIsDeletedFalse(loanId, userId))
            throw new ResourceNotFoundException("Loan doesn't exist");

        LoanSchedule loanSchedule = loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, paymentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment schedule not found"));

        return LoanScheduleResponse.of(loanSchedule);
    }

    @Transactional
    public LoanPaymentResponse payInstallment(UUID loanId, int paymentNumber,
                               LoanPaymentRequest loanPaymentRequest, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        Loan loan = loanRepository.findByIdAndUserIdAndIsDeletedFalseAndStatus(loanId, userId, LoanStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Loan doesn't exist"));


        LoanSchedule loanSchedule = loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, paymentNumber)
                .orElseThrow( () -> new ResourceNotFoundException("Payment schedule not found"));


        Optional<Transaction> existingTransaction = transactionRepository
                .findByIdempotencyKey(loanPaymentRequest.idempotencyKey());

        if(existingTransaction.isPresent()){
            return generateLoanPaymentResponse(loan, loanSchedule, existingTransaction.get());
        }

        if(loanSchedule.getStatus() == LoanScheduleStatus.PAID) throw new BadRequestException("This installment has already been paid");

        boolean hasPendingPreviousPayments = loanScheduleRepository
                .existsByLoanIdAndPaymentNumberLessThanAndStatus(loanId, paymentNumber, LoanScheduleStatus.PENDING);

        if(hasPendingPreviousPayments) throw new BadRequestException("Previous installment must be paid before this one");

        Account account = loan.getAccount();

        if(!account.getIsActive() || account.getBalance().compareTo(loanSchedule.getPaymentAmount()) < 0)
            throw new BadRequestException("Account must be active and have enough balance");

        account.setBalance(account.getBalance()
                .subtract(loanSchedule.getPaymentAmount()));

        loanSchedule.setStatus(LoanScheduleStatus.PAID);

        Transaction transactionCopyOfThePayment = Transaction.builder()
                .account(account)
                .loan(loan)
                .referenceNumber(referenceNumberGenerator.generate())
                .type(TransactionType.LOAN_PAYMENT)
                .amount(loanSchedule.getPaymentAmount())
                .category("LOAN_REPAYMENT")
                .idempotencyKey(loanPaymentRequest.idempotencyKey())
                .transactedAt(Instant.now())
                .build();

        boolean isThereARemainingPendingPaymentSchedule = loanScheduleRepository
                .existsByLoanIdAndPaymentNumberGreaterThanAndStatus(loanId, paymentNumber, LoanScheduleStatus.PENDING);

        if(!isThereARemainingPendingPaymentSchedule){
            loan.setStatus(LoanStatus.PAID_OFF);
        }

        transactionRepository.save(transactionCopyOfThePayment);
        loanScheduleRepository.save(loanSchedule);
        loanRepository.save(loan);
        accountRepository.save(account);

        return generateLoanPaymentResponse(loan, loanSchedule, transactionCopyOfThePayment);
    }

    private LoanPaymentResponse generateLoanPaymentResponse (Loan loan, LoanSchedule loanSchedule, Transaction transaction){

        return new LoanPaymentResponse(LoanResponse.of(loan),
                LoanScheduleResponse.of(loanSchedule),
                TransactionResponse.of(transaction));
    }
}
