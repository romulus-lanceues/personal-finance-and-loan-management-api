package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.config.AuthenticationPrincipalTestConfig;
import com.lancea.personal_finance_loan_api.config.TestSecurityConfig;
import com.lancea.personal_finance_loan_api.dto.request.LoanRequest;
import com.lancea.personal_finance_loan_api.dto.response.*;
import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;
import com.lancea.personal_finance_loan_api.enums.LoanStatus;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.service.LoanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
@Import({AuthenticationPrincipalTestConfig.class, TestSecurityConfig.class})
public class LoanControllerTest {

    private static final String TEST_USER_ID = "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    LoanService loanService;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    CacheManager cacheManager;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("createLoan tests")
    class CreateLoan {

        @Test
        @DisplayName("given valid loan details when create loan then return 201 with location and loan details")
        void givenValidLoanDetails_whenCreateLoan_thenReturnLoanDetails() throws Exception {
            LoanRequest loanRequest = new LoanRequest(
                    UUID.randomUUID(),
                    "Test loan",
                    BigDecimal.valueOf(200000),
                    BigDecimal.valueOf(2.4),
                    36,
                    LocalDate.now().minusDays(3)
            );

            LoanResponse loanResponse = new LoanResponse(
                    UUID.randomUUID(),
                    loanRequest.loanName(),
                    loanRequest.accountId(),
                    loanRequest.principal(),
                    loanRequest.annualRate(),
                    BigDecimal.valueOf(2000),
                    loanRequest.termMonths(),
                    LoanStatus.ACTIVE,
                    loanRequest.disbursedAt(),
                    LocalDate.now().plusMonths(36)
            );

            given(loanService.createLoan(eq(loanRequest), any(Jwt.class))).willReturn(loanResponse);

            mockMvc.perform(post("/api/v1/loans")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loanRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/v1/loans/" + loanResponse.loanId()))
                    .andExpect(jsonPath("$.loanId").value(String.valueOf(loanResponse.loanId())))
                    .andExpect(jsonPath("$.loanName").value(loanRequest.loanName()))
                    .andExpect(jsonPath("$.accountId").value(loanRequest.accountId().toString()))
                    .andExpect(jsonPath("$.principal").value(200000))
                    .andExpect(jsonPath("$.annualRate").value(2.4))
                    .andExpect(jsonPath("$.termMonths").value(36))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(loanService).createLoan(eq(loanRequest), any(Jwt.class));
        }

        @Test
        @DisplayName("given invalid loan details when create loan then return 400 Bad Request")
        void givenInvalidLoanDetails_whenCreateLoan_thenReturnBadRequest() throws Exception {
            String invalidRequest = """
                    {"accountId" : "", "loanName": "Invalid Loan", 
                    "principal": "23432", 
                    "annualRate": "3.0", 
                    "termMonths": "12", 
                    "disbursedAt": "2018-01-01"}
                    """;

            mockMvc.perform(post("/api/v1/loans")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(loanService);
        }

        @Test
        @DisplayName("given no jwt token when create loan then return 401 Unauthorized")
        void givenNoJwtToken_whenCreateLoan_thenReturnUnauthorized() throws Exception {
            LoanRequest loanRequest = new LoanRequest(
                    UUID.randomUUID(),
                    "Test loan",
                    BigDecimal.valueOf(200000),
                    BigDecimal.valueOf(2.4),
                    36,
                    LocalDate.now().minusDays(3)
            );

            mockMvc.perform(post("/api/v1/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loanRequest)))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(loanService);
        }
    }

    @Nested
    @DisplayName("getUserLoans tests")
    class GetUserLoans {

        @Test
        @DisplayName("given valid jwt and default pagination when get user loans then return 200 with paged response")
        void givenValidJwtAndDefaultPageable_whenGetUserLoans_thenReturnPagedLoans() throws Exception {
            UUID loanId = UUID.randomUUID();
            LoanResponse loanResponse = new LoanResponse(
                    loanId,
                    "Auto Loan",
                    UUID.randomUUID(),
                    BigDecimal.valueOf(500000),
                    BigDecimal.valueOf(5.5),
                    BigDecimal.valueOf(15000),
                    36,
                    LoanStatus.ACTIVE,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2029, 1, 1)
            );

            PagedLoanResponse pagedLoanResponse = new PagedLoanResponse(
                    List.of(loanResponse),
                    1L,
                    1,
                    0,
                    10,
                    true,
                    true
            );

            given(loanService.getUserLoans(any(Pageable.class), any(Jwt.class))).willReturn(pagedLoanResponse);

            mockMvc.perform(get("/api/v1/loans")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].loanId").value(loanId.toString()))
                    .andExpect(jsonPath("$.content[0].loanName").value("Auto Loan"))
                    .andExpect(jsonPath("$.content[0].principal").value(500000))
                    .andExpect(jsonPath("$.content[0].annualRate").value(5.5))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(loanService).getUserLoans(pageableCaptor.capture(), any(Jwt.class));

            Pageable capturedPageable = pageableCaptor.getValue();
            assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
            assertThat(capturedPageable.getPageSize()).isEqualTo(10);
            assertThat(capturedPageable.getSort().getOrderFor("disbursedAt")).isNotNull();
            assertThat(capturedPageable.getSort().getOrderFor("disbursedAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("given custom pageable params when get user loans then pass bound pageable to service")
        void givenCustomPageableParams_whenGetUserLoans_thenPassPageableToService() throws Exception {
            PagedLoanResponse emptyResponse = new PagedLoanResponse(
                    List.of(),
                    0L,
                    0,
                    2,
                    5,
                    false,
                    true
            );

            given(loanService.getUserLoans(any(Pageable.class), any(Jwt.class))).willReturn(emptyResponse);

            mockMvc.perform(get("/api/v1/loans")
                            .param("page", "2")
                            .param("size", "5")
                            .param("sort", "disbursedAt,desc")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value(2))
                    .andExpect(jsonPath("$.size").value(5));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(loanService).getUserLoans(pageableCaptor.capture(), any(Jwt.class));

            Pageable capturedPageable = pageableCaptor.getValue();
            assertThat(capturedPageable.getPageNumber()).isEqualTo(2);
            assertThat(capturedPageable.getPageSize()).isEqualTo(5);
            assertThat(capturedPageable.getSort().getOrderFor("disbursedAt")).isNotNull();
            assertThat(capturedPageable.getSort().getOrderFor("disbursedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("given no jwt token when get user loans then return 401 Unauthorized")
        void givenNoJwtToken_whenGetUserLoans_thenReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/loans"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(loanService);
        }
    }

    @Nested
    @DisplayName("getLoanById tests")
    class GetLoanById {

        @Test
        @DisplayName("given existing loan id when get loan by id then return 200 with loan details")
        void givenExistingLoanId_whenGetLoanById_thenReturnLoanDetails() throws Exception {
            UUID loanId = UUID.randomUUID();
            LoanResponse loanResponse = new LoanResponse(
                    loanId,
                    "Personal Loan",
                    UUID.randomUUID(),
                    BigDecimal.valueOf(100000),
                    BigDecimal.valueOf(4.0),
                    BigDecimal.valueOf(3000),
                    36,
                    LoanStatus.ACTIVE,
                    LocalDate.of(2026, 2, 1),
                    LocalDate.of(2029, 2, 1)
            );

            given(loanService.getLoanById(eq(loanId), any(Jwt.class))).willReturn(loanResponse);

            mockMvc.perform(get("/api/v1/loans/{loanId}", loanId)
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loanId").value(loanId.toString()))
                    .andExpect(jsonPath("$.loanName").value("Personal Loan"))
                    .andExpect(jsonPath("$.principal").value(100000))
                    .andExpect(jsonPath("$.annualRate").value(4.0))
                    .andExpect(jsonPath("$.monthlyPayment").value(3000))
                    .andExpect(jsonPath("$.termMonths").value(36))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.disbursedAt").value("2026-02-01"))
                    .andExpect(jsonPath("$.maturityDate").value("2029-02-01"));

            verify(loanService).getLoanById(eq(loanId), any(Jwt.class));
        }

        @Test
        @DisplayName("given non-existent loan id when get loan by id then return 404 Not Found")
        void givenNonExistentLoanId_whenGetLoanById_thenReturnNotFound() throws Exception {
            UUID loanId = UUID.randomUUID();
            given(loanService.getLoanById(eq(loanId), any(Jwt.class)))
                    .willThrow(new ResourceNotFoundException("Loan does not exist"));

            mockMvc.perform(get("/api/v1/loans/{loanId}", loanId)
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Loan does not exist"));

            verify(loanService).getLoanById(eq(loanId), any(Jwt.class));
        }

        @Test
        @DisplayName("given invalid loan id format when get loan by id then return 400 Bad Request")
        void givenInvalidLoanIdFormat_whenGetLoanById_thenReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/loans/{loanId}", "invalid-uuid")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(loanService);
        }

        @Test
        @DisplayName("given no jwt token when get loan by id then return 401 Unauthorized")
        void givenNoJwtToken_whenGetLoanById_thenReturnUnauthorized() throws Exception {
            UUID loanId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/loans/{loanId}", loanId))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(loanService);
        }
    }

    @Nested
    @DisplayName("compareLoans tests")
    class CompareLoans {

        @Test
        @DisplayName("given valid loan ids when compare loans then return 200 with comparison response")
        void givenValidLoanIds_whenCompareLoans_thenReturnComparisonResult() throws Exception {
            UUID loanAId = UUID.randomUUID();
            UUID loanBId = UUID.randomUUID();

            LoanDetails loanADetails = new LoanDetails(
                    "Car Loan",
                    loanAId,
                    BigDecimal.valueOf(4.5),
                    BigDecimal.valueOf(320000.00),
                    BigDecimal.valueOf(20000.00)
            );

            LoanDetails loanBDetails = new LoanDetails(
                    "Personal Loan",
                    loanBId,
                    BigDecimal.valueOf(6.0),
                    BigDecimal.valueOf(350000.00),
                    BigDecimal.valueOf(50000.00)
            );

            LoanComparisonResponse response = new LoanComparisonResponse(
                    loanADetails,
                    loanBDetails,
                    BigDecimal.valueOf(30000.00),
                    BigDecimal.valueOf(833.33),
                    "Car Loan"
            );

            given(loanService.compareLoan(eq(loanAId), eq(loanBId), any(Jwt.class))).willReturn(response);

            mockMvc.perform(get("/api/v1/loans/compare")
                            .param("loanAId", loanAId.toString())
                            .param("loanBId", loanBId.toString())
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loanA.loanId").value(loanAId.toString()))
                    .andExpect(jsonPath("$.loanA.loanName").value("Car Loan"))
                    .andExpect(jsonPath("$.loanA.annualRate").value(4.5))
                    .andExpect(jsonPath("$.loanA.amountPayable").value(320000.00))
                    .andExpect(jsonPath("$.loanA.totalInterest").value(20000.00))
                    .andExpect(jsonPath("$.loanB.loanId").value(loanBId.toString()))
                    .andExpect(jsonPath("$.loanB.loanName").value("Personal Loan"))
                    .andExpect(jsonPath("$.loanB.annualRate").value(6.0))
                    .andExpect(jsonPath("$.loanB.amountPayable").value(350000.00))
                    .andExpect(jsonPath("$.loanB.totalInterest").value(50000.00))
                    .andExpect(jsonPath("$.interestDifference").value(30000.00))
                    .andExpect(jsonPath("$.monthlyPaymentDifference").value(833.33))
                    .andExpect(jsonPath("$.loanWithLowerCost").value("Car Loan"));

            verify(loanService).compareLoan(eq(loanAId), eq(loanBId), any(Jwt.class));
        }

        @Test
        @DisplayName("given non-existent loan id when compare loans then return 404 Not Found")
        void givenNonExistentLoanId_whenCompareLoans_thenReturnNotFound() throws Exception {
            UUID loanAId = UUID.randomUUID();
            UUID loanBId = UUID.randomUUID();

            given(loanService.compareLoan(eq(loanAId), eq(loanBId), any(Jwt.class)))
                    .willThrow(new ResourceNotFoundException("Loan with an ID of: " + loanAId + " not found"));

            mockMvc.perform(get("/api/v1/loans/compare")
                            .param("loanAId", loanAId.toString())
                            .param("loanBId", loanBId.toString())
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Loan with an ID of: " + loanAId + " not found"));

            verify(loanService).compareLoan(eq(loanAId), eq(loanBId), any(Jwt.class));
        }

        @Test
        @DisplayName("given missing request param when compare loans then return 400 Bad Request")
        void givenMissingRequestParam_whenCompareLoans_thenReturnBadRequest() throws Exception {
            UUID loanAId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/loans/compare")
                            .param("loanAId", loanAId.toString())
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(loanService);
        }

        @Test
        @DisplayName("given invalid uuid request param when compare loans then return 400 Bad Request")
        void givenInvalidUuidParam_whenCompareLoans_thenReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/loans/compare")
                            .param("loanAId", "invalid-uuid")
                            .param("loanBId", UUID.randomUUID().toString())
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(loanService);
        }

        @Test
        @DisplayName("given no jwt token when compare loans then return 401 Unauthorized")
        void givenNoJwtToken_whenCompareLoans_thenReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/loans/compare")
                            .param("loanAId", UUID.randomUUID().toString())
                            .param("loanBId", UUID.randomUUID().toString()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(loanService);
        }
    }

    @Nested
    @DisplayName("simulatePayment tests")
    class SimulatePayment {

        @Test
        @DisplayName("given valid simulation params when simulate payment then return 200 with simulation result")
        void givenValidParameters_whenSimulatePayment_thenReturnSimulationResponse() throws Exception {
            UUID loanId = UUID.randomUUID();
            int paymentNumber = 3;
            BigDecimal extraAmount = BigDecimal.valueOf(500.00);

            LoanScheduleResponse scheduleItem = new LoanScheduleResponse(
                UUID.randomUUID(),
                loanId,
                4,
                BigDecimal.valueOf(1200.00),
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(200.00),
                BigDecimal.valueOf(5000.00),
                LocalDate.of(2026, 5, 1),
                LoanScheduleStatus.PENDING
            );

            LoanSimulationResponse response = new LoanSimulationResponse(
                List.of(scheduleItem),
                BigDecimal.valueOf(450.00),
                2
            );

            given(loanService.simulatePayment(
                    eq(loanId),
                    eq(paymentNumber),
                    argThat(amount -> amount != null && amount.compareTo(extraAmount) == 0),
                    any(Jwt.class)
            )).willReturn(response);

            mockMvc.perform(post("/api/v1/loans/{loanId}/simulate", loanId)
                            .param("paymentNumber", String.valueOf(paymentNumber))
                            .param("extraAmount", "500.00")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.interestSaved").value(450.00))
                    .andExpect(jsonPath("$.monthsSaved").value(2))
                    .andExpect(jsonPath("$.simulatedSchedule.length()").value(1))
                    .andExpect(jsonPath("$.simulatedSchedule[0].paymentNumber").value(4))
                    .andExpect(jsonPath("$.simulatedSchedule[0].paymentAmount").value(1200.00))
                    .andExpect(jsonPath("$.simulatedSchedule[0].principalPortion").value(1000.00))
                    .andExpect(jsonPath("$.simulatedSchedule[0].interestPortion").value(200.00))
                    .andExpect(jsonPath("$.simulatedSchedule[0].remainingBalance").value(5000.00))
                    .andExpect(jsonPath("$.simulatedSchedule[0].dueDate").value("2026-05-01"))
                    .andExpect(jsonPath("$.simulatedSchedule[0].loanScheduleStatus").value("PENDING"));

            verify(loanService).simulatePayment(
                    eq(loanId),
                    eq(paymentNumber),
                    argThat(amount -> amount != null && amount.compareTo(extraAmount) == 0),
                    any(Jwt.class)
            );
        }

        @Test
        @DisplayName("given non-positive extra amount when simulate payment then return 400 Bad Request")
        void givenNonPositiveExtraAmount_whenSimulatePayment_thenReturnBadRequest() throws Exception {
            UUID loanId = UUID.randomUUID();

            given(loanService.simulatePayment(
                    eq(loanId),
                    eq(3),
                    argThat(amount -> amount != null && amount.compareTo(BigDecimal.valueOf(-100.00)) == 0),
                    any(Jwt.class)
            )).willThrow(new BadRequestException("Extra amount must be greater than zero"));

            mockMvc.perform(post("/api/v1/loans/{loanId}/simulate", loanId)
                            .param("paymentNumber", "3")
                            .param("extraAmount", "-100.00")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Extra amount must be greater than zero"));

            verify(loanService).simulatePayment(
                    eq(loanId),
                    eq(3),
                    argThat(amount -> amount != null && amount.compareTo(BigDecimal.valueOf(-100.00)) == 0),
                    any(Jwt.class)
            );
        }

        @Test
        @DisplayName("given already paid installment when simulate payment then return 400 Bad Request")
        void givenPaidInstallment_whenSimulatePayment_thenReturnBadRequest() throws Exception {
            UUID loanId = UUID.randomUUID();

            given(loanService.simulatePayment(
                    eq(loanId),
                    eq(1),
                    argThat(amount -> amount != null && amount.compareTo(BigDecimal.valueOf(500.00)) == 0),
                    any(Jwt.class)
            )).willThrow(new BadRequestException("Cannot simulate against an installment that is already paid"));

            mockMvc.perform(post("/api/v1/loans/{loanId}/simulate", loanId)
                            .param("paymentNumber", "1")
                            .param("extraAmount", "500.00")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Cannot simulate against an installment that is already paid"));

            verify(loanService).simulatePayment(
                    eq(loanId),
                    eq(1),
                    argThat(amount -> amount != null && amount.compareTo(BigDecimal.valueOf(500.00)) == 0),
                    any(Jwt.class)
            );
        }

        @Test
        @DisplayName("given non-existent loan or schedule when simulate payment then return 404 Not Found")
        void givenNonExistentLoanOrSchedule_whenSimulatePayment_thenReturnNotFound() throws Exception {
            UUID loanId = UUID.randomUUID();

            given(loanService.simulatePayment(
                    eq(loanId),
                    eq(99),
                    argThat(amount -> amount != null && amount.compareTo(BigDecimal.valueOf(500.00)) == 0),
                    any(Jwt.class)
            )).willThrow(new ResourceNotFoundException("Payment number not found in schedule"));

            mockMvc.perform(post("/api/v1/loans/{loanId}/simulate", loanId)
                            .param("paymentNumber", "99")
                            .param("extraAmount", "500.00")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Payment number not found in schedule"));

            verify(loanService).simulatePayment(
                    eq(loanId),
                    eq(99),
                    argThat(amount -> amount != null && amount.compareTo(BigDecimal.valueOf(500.00)) == 0),
                    any(Jwt.class)
            );
        }

        @Test
        @DisplayName("given missing required request param when simulate payment then return 400 Bad Request")
        void givenMissingRequestParam_whenSimulatePayment_thenReturnBadRequest() throws Exception {
            UUID loanId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/loans/{loanId}/simulate", loanId)
                            .param("paymentNumber", "3")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(loanService);
        }

        @Test
        @DisplayName("given invalid param type when simulate payment then return 400 Bad Request")
        void givenInvalidParamType_whenSimulatePayment_thenReturnBadRequest() throws Exception {
            UUID loanId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/loans/{loanId}/simulate", loanId)
                            .param("paymentNumber", "not-an-int")
                            .param("extraAmount", "500.00")
                            .with(jwt().jwt(j -> j.claim("userId", TEST_USER_ID))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(loanService);
        }

        @Test
        @DisplayName("given no jwt token when simulate payment then return 401 Unauthorized")
        void givenNoJwtToken_whenSimulatePayment_thenReturnUnauthorized() throws Exception {
            UUID loanId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/loans/{loanId}/simulate", loanId)
                            .param("paymentNumber", "3")
                            .param("extraAmount", "500.00"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(loanService);
        }
    }
}
