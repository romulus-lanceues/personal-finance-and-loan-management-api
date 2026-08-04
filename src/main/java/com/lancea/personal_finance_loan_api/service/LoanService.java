package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.LoanRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanResponse;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.Loan;
import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.LoanRepository;
import com.lancea.personal_finance_loan_api.repository.LoanScheduleRepository;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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


        BigDecimal monthlyPayment;
        BigDecimal monthlyRate = BigDecimal.ZERO;

        if(request.annualRate().compareTo(BigDecimal.ZERO) == 0){
            monthlyPayment = request.principal().divide(BigDecimal.valueOf(request.termMonths()), SCALE, ROUNDING_MODE);
        }
        else {
            List<BigDecimal> monthlyRateAndPayment = calculateMonthlyRateAndPayment(request);
            monthlyRate = monthlyRateAndPayment.getFirst();
            monthlyPayment = monthlyRateAndPayment.getLast();
        }

        Loan loan = Loan.builder()
                .user(user)
                .account(account)
                .loanName(request.loanName())
                .principal(request.principal())
                .annualRate(request.annualRate())
                .termMonths(request.termMonths())
                .monthlyPayment(monthlyPayment)
                .disbursedAt(request.disbursedAt())
                .maturityDate(request.disbursedAt().plus(Duration.ofDays( (long) 30 * request.termMonths())))
                .build();

        loanRepository.save(loan);

        List<LoanSchedule> generatedLoanSchedules = createLoanScheduleForInterests(request.principal(), request.termMonths(), request.disbursedAt(), monthlyRate, monthlyPayment, loan);

        loanScheduleRepository.saveAll(generatedLoanSchedules);


        return new LoanResponse(loan.getId(), loan.getLoanName(), loan.getCreatedAt());
    }


    private List<BigDecimal> calculateMonthlyRateAndPayment(LoanRequest request){

        BigDecimal monthlyRate = request.annualRate()
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE)
                .divide(BigDecimal.valueOf(12), SCALE, ROUNDING_MODE);

        BigDecimal monthlyRatePlusOneToThePowOfTerm = BigDecimal.ONE
                .add(monthlyRate)
                .pow(request.termMonths());

        BigDecimal numerator = monthlyRate.multiply(monthlyRatePlusOneToThePowOfTerm);

        BigDecimal denominator = monthlyRatePlusOneToThePowOfTerm.subtract(BigDecimal.ONE);

        BigDecimal factor = numerator.divide(denominator, SCALE, ROUNDING_MODE);

        BigDecimal monthlyPayment = request.principal().multiply(factor).setScale(4, ROUNDING_MODE);

        return List.of(monthlyRate, monthlyPayment);

    }

    private List<LoanSchedule> createLoanScheduleForInterests(BigDecimal principal, int termMonths,
                                                              Instant disbursedAt, BigDecimal monthlyRate,
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

            LocalDate dueDate = disbursedAt.atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .plusMonths(paymentNumber);

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

}
