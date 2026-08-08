package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.response.LoanScheduleResponse;
import com.lancea.personal_finance_loan_api.entity.LoanSchedule;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.LoanRepository;
import com.lancea.personal_finance_loan_api.repository.LoanScheduleRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanScheduleService {

    private final LoanScheduleRepository loanScheduleRepository;
    private final LoanRepository loanRepository;

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
}
