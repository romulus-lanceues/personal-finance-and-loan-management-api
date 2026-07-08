package com.lancea.personal_finance_loan_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PersonalFinanceLoanApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalFinanceLoanApiApplication.class, args);
	}

}
