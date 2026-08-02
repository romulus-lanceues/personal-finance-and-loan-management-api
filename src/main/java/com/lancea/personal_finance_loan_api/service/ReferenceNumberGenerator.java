package com.lancea.personal_finance_loan_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ReferenceNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public String generate(){
        Long seq  = jdbcTemplate.queryForObject("SELECT NEXTVAL('txn_sequence')", Long.class);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("TXN-%s-%05d", date, seq);
    }
}
