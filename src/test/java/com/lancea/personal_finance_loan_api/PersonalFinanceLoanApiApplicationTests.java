package com.lancea.personal_finance_loan_api;

import com.lancea.personal_finance_loan_api.config.TestInfrastructureConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestInfrastructureConfig.class)
class PersonalFinanceLoanApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
