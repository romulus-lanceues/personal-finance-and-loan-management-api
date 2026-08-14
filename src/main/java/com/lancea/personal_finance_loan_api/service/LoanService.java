package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.aspect.Auditable;
import com.lancea.personal_finance_loan_api.dto.request.LoanRequest;
import com.lancea.personal_finance_loan_api.dto.response.*;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.Loan;
import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.LoanRepository;
import com.lancea.personal_finance_loan_api.repository.LoanScheduleRepository;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository loanScheduleRepository;

    @Transactional
    public LoanResponse createLoan(LoanRequest request, Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        User user = userRepository.findById(userId)
                .orElseThrow( () -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(request.accountId(), userId)
                .orElseThrow( () -> new ResourceNotFoundException("Account doesn't exist"));


        BigDecimal monthlyRate = computeMonthlyRate(request.annualRate());
        BigDecimal monthlyPayment = determineMonthlyPayment(request.annualRate(), request.principal(),
                request.termMonths(), monthlyRate);

        Loan loan = Loan.builder()
                .user(user)
                .account(account)
                .loanName(request.loanName())
                .principal(request.principal())
                .annualRate(request.annualRate())
                .termMonths(request.termMonths())
                .monthlyPayment(monthlyPayment)
                .disbursedAt(request.disbursedAt())
                .maturityDate(request.disbursedAt().plusMonths(request.termMonths()))
                .build();

        loanRepository.save(loan);

        List<LoanSchedule> generatedLoanSchedules = createLoanSchedules(request.principal(), request.termMonths(),
                request.disbursedAt(), monthlyRate, monthlyPayment, loan);

        loanScheduleRepository.saveAll(generatedLoanSchedules);


        return new LoanResponse(loan.getId(), loan.getLoanName(), loan.getCreatedAt());
    }

    private BigDecimal determineMonthlyPayment(BigDecimal annualRate, BigDecimal principal,
                                               int termMonths, BigDecimal monthlyRate){

        if(annualRate.compareTo(BigDecimal.ZERO) == 0){
             return principal.divide(BigDecimal.valueOf(termMonths), SCALE, ROUNDING_MODE);
        }
        else {
            return calculateMonthlyPayment(principal, monthlyRate, termMonths);
        }

    }


    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate,
                                                            int termMonths){

        BigDecimal monthlyRatePlusOneToThePowOfTerm = BigDecimal.ONE
                .add(monthlyRate)
                .pow(termMonths);

        BigDecimal numerator = monthlyRate.multiply(monthlyRatePlusOneToThePowOfTerm);

        BigDecimal denominator = monthlyRatePlusOneToThePowOfTerm.subtract(BigDecimal.ONE);

        BigDecimal factor = numerator.divide(denominator, SCALE, ROUNDING_MODE);

        return principal.multiply(factor).setScale(4, ROUNDING_MODE);
    }


    private List<LoanSchedule> createLoanSchedules(BigDecimal principal, int termMonths,
                                                              LocalDate disbursedAt, BigDecimal monthlyRate,
                                                              BigDecimal monthlyPayment, Loan loan){
        BigDecimal remainingBalance = principal;

        List<LoanSchedule> monthlySchedule = new ArrayList<>();

        for(int paymentNumber = 1; paymentNumber <= termMonths; paymentNumber++){

            BigDecimal interestPortion = remainingBalance
                    .multiply(monthlyRate)
                    .setScale(4, ROUNDING_MODE );

            BigDecimal principalPortion = monthlyPayment
                    .subtract(interestPortion)
                    .setScale(4, ROUNDING_MODE);

            remainingBalance = remainingBalance
                    .subtract(principalPortion)
                    .setScale(4, ROUNDING_MODE);

            LocalDate dueDate = disbursedAt.plusMonths(paymentNumber);

            monthlySchedule.add(LoanSchedule.builder()
                            .loan(loan)
                            .paymentNumber(paymentNumber)
                            .paymentAmount(monthlyPayment)
                            .principalPortion(principalPortion)
                            .interestPortion(interestPortion)
                            .remainingBalance(remainingBalance)
                            .dueDate(dueDate)
                            .build());

        }

        LoanSchedule lastRow = monthlySchedule.getLast();

        if(lastRow.getRemainingBalance().compareTo(BigDecimal.ZERO) != 0){
            BigDecimal adjustment = lastRow.getRemainingBalance();

            lastRow.setPrincipalPortion(lastRow.getPrincipalPortion().add(adjustment));

            lastRow.setPaymentAmount(lastRow.getPaymentAmount().add(adjustment));

            lastRow.setRemainingBalance(BigDecimal.ZERO);
        }

        return monthlySchedule;
    }

    @Auditable(action = "view", entityType = "loan")
    public PagedLoanResponse getUserLoans(Pageable pageable, Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        Page<Loan> loans = loanRepository.findByUserIdAndIsDeletedIsFalse(userId,pageable);

        return PagedLoanResponse.of(loans);
    }

    public LoanResponse getLoanById(UUID loanId, Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        Loan loan = loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId)
                .orElseThrow( () -> new ResourceNotFoundException("Loan does not exist"));

        return LoanResponse.of(loan);
    }

    public LoanComparisonResponse compareLoan(UUID loanAId, UUID loanBId, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        Loan loanA = loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanAId, userId)
                .orElseThrow( () -> new ResourceNotFoundException("Loan with an ID of: " + loanAId + "not found"));

        Loan loanB = loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanBId, userId)
                .orElseThrow( () -> new ResourceNotFoundException("Loan with an ID of: " + loanAId + "not found"));


       LoanDetails loanADetails = calculateLoanDetails(loanA);
       LoanDetails loanBDetails = calculateLoanDetails(loanB);


        BigDecimal interestDifference = loanADetails.totalInterest()
                .subtract( loanBDetails.totalInterest())
                .setScale(2, ROUNDING_MODE)
                .abs();

        BigDecimal monthlyDifference = loanA.getMonthlyPayment()
                .subtract(loanB.getMonthlyPayment())
                .setScale(2, ROUNDING_MODE)
                .abs();


        String loanWithLowerCost =

                //Compares loanA amount to loanB, converts the value to its integer representation
                //then identify which value cost less base on it

                switch (Integer.signum(loanADetails.amountPayable().compareTo(loanBDetails.amountPayable()))) {
                    case  1 -> loanB.getLoanName();
                    case -1 -> loanA.getLoanName();
                    default -> "Both loans have equal payable amount";
                };


        return new LoanComparisonResponse (loanADetails, loanBDetails, interestDifference, monthlyDifference, loanWithLowerCost);
    }

    private LoanDetails calculateLoanDetails(Loan loan) {

        BigDecimal totalAmountPayable = loan.getMonthlyPayment()
                .multiply(BigDecimal.valueOf(loan.getTermMonths()))
                .setScale(2, ROUNDING_MODE);
        BigDecimal totalInterestPaid = totalAmountPayable
                .subtract(loan.getPrincipal())
                .setScale(2, ROUNDING_MODE);

        return new LoanDetails(loan.getLoanName(),
                loan.getId(),
                loan.getAnnualRate(),
                totalAmountPayable,
                totalInterestPaid);
    }

    public LoanSimulationResponse simulatePayment (UUID loanId, int paymentNumber,
                             BigDecimal extraAmount, Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        Loan loan = loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId)
                .orElseThrow( () -> new RuntimeException("Loan not found"));

        validateExtraAmount(extraAmount);

        LoanSchedule targetRow = loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, paymentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment number not found in schedule"));

        validateRow(targetRow);

        BigDecimal monthlyRate = computeMonthlyRate(loan.getAnnualRate());
        BigDecimal monthlyPayment = loan.getMonthlyPayment();

        BigDecimal remainingBalance = targetRow.getRemainingBalance().subtract(extraAmount);


        if(remainingBalance.compareTo(BigDecimal.ZERO) <= 0){
            return buildFullPayoffResponse(loan, paymentNumber);
        }

        List<LoanSchedule> simulatedSchedule = new ArrayList<>();
        LocalDate disbursedDate = loan.getDisbursedAt();

        int simulatedPaymentNumber = paymentNumber + 1;
        int maxIterations = loan.getTermMonths() - paymentNumber;
        int iterations = 0;

        while(remainingBalance.compareTo(BigDecimal.ZERO) > 0 &&
                 iterations < maxIterations){

            iterations++;

            BigDecimal interestPortion = remainingBalance
                    .multiply(monthlyRate)
                    .setScale(4, ROUNDING_MODE);

            BigDecimal principalPortion = monthlyPayment
                    .subtract(interestPortion)
                    .setScale(4, ROUNDING_MODE);

            BigDecimal actualPayment = monthlyPayment;


            if(principalPortion.compareTo(remainingBalance) >= 0){
                principalPortion = remainingBalance;
                actualPayment = principalPortion.add(interestPortion).setScale(4, ROUNDING_MODE);
                remainingBalance = BigDecimal.ZERO;
            }
            else {
                remainingBalance = remainingBalance.subtract(principalPortion).setScale(4, ROUNDING_MODE);
            }

            LocalDate dueDate = disbursedDate.plusMonths(simulatedPaymentNumber);

            simulatedSchedule.add(LoanSchedule.builder()
                            .loan(loan)
                            .paymentNumber(simulatedPaymentNumber)
                            .paymentAmount(actualPayment)
                            .principalPortion(principalPortion)
                            .interestPortion(interestPortion)
                            .remainingBalance(remainingBalance)
                            . dueDate(dueDate)
                            .status(LoanScheduleStatus.PENDING)
                            .build());

            simulatedPaymentNumber++;

        }

        return buildSimulationResponse(loan, paymentNumber, extraAmount, simulatedSchedule);
}

    private void validateExtraAmount(BigDecimal extraAmount){
        if (extraAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Extra amount must be greater than zero");
        }
    }

    private void validateRow(LoanSchedule targetRow){
        if (targetRow.getStatus() != LoanScheduleStatus.PENDING) {
            throw new BadRequestException("Cannot simulate against an installment that is already paid");
        }
    }

    private LoanSimulationResponse buildFullPayoffResponse(Loan loan, int startPaymentNumber){

        List<LoanSchedule> remainingActualLoanSchedule = loanScheduleRepository.findByLoanId(loan.getId())
                .stream()
                .filter(loanSchedule -> loanSchedule.getPaymentNumber() > startPaymentNumber)
                .toList();

        BigDecimal savedInterest = remainingActualLoanSchedule
                .stream()
                .map(LoanSchedule::getInterestPortion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int monthsSaved = remainingActualLoanSchedule.size();

        return new LoanSimulationResponse(new ArrayList<>(), savedInterest, monthsSaved);
    }

    private LoanSimulationResponse buildSimulationResponse(Loan loan, int startPaymentNumber,
                                                           BigDecimal extraAmount, List<LoanSchedule>simulatedSchedule){

        List<LoanSchedule> remainingActualLoanSchedule = loanScheduleRepository.findByLoanId(loan.getId())
                .stream()
                .filter( loanSchedule -> loanSchedule.getPaymentNumber() > startPaymentNumber)
                .toList();

        BigDecimal originalRemainingInterest = remainingActualLoanSchedule
                .stream()
                .map(LoanSchedule::getInterestPortion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal simulatedInterest = simulatedSchedule
                .stream()
                .map(LoanSchedule::getInterestPortion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal interestSaved = originalRemainingInterest.subtract(simulatedInterest);

        int monthsSaved = remainingActualLoanSchedule.size() - simulatedSchedule.size();

        List<LoanScheduleResponse> loanScheduleResponses = simulatedSchedule
                .stream()
                .map(LoanScheduleResponse::of)
                .toList();

        return new LoanSimulationResponse(loanScheduleResponses, interestSaved, monthsSaved);
    }


    private BigDecimal computeMonthlyRate(BigDecimal annualRate){

        if(annualRate.compareTo(BigDecimal.ZERO) < 0) throw new BadRequestException("Interest must not be negative");

        if(annualRate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return annualRate
                .divide(BigDecimal.valueOf(12), SCALE, ROUNDING_MODE)
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
    }
}
