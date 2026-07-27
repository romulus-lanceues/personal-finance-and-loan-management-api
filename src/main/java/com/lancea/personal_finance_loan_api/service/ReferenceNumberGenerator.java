package com.lancea.personal_finance_loan_api.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReferenceNumberGenerator {

    private final AtomicInteger sequence = new AtomicInteger(66);

    public String generate(){
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = sequence.incrementAndGet();
        return String.format("TXN-%s-%05d", date, seq);
    }
}
