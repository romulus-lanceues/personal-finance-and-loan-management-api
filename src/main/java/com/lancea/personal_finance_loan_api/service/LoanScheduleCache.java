package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.response.LoanScheduleResponse;
import com.lancea.personal_finance_loan_api.repository.LoanScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LoanScheduleCache {

    private final LoanScheduleRepository loanScheduleRepository;

    @Cacheable(value = "loan-schedule", key = "#loanId")
    public List<LoanScheduleResponse> getSchedules(UUID loanId){

        //Retrieve the LoanSchedules and convert them into LoanScheduleResponses
        return loanScheduleRepository
                .findByLoanId(loanId)
                .stream()
                .map(LoanScheduleResponse::of)
                .collect(Collectors.toList());
    }
}
