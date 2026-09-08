package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.LoanRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanComparisonResponse;
import com.lancea.personal_finance_loan_api.dto.response.LoanResponse;
import com.lancea.personal_finance_loan_api.dto.response.LoanSimulationResponse;
import com.lancea.personal_finance_loan_api.dto.response.PagedLoanResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanScheduleRepository loanScheduleRepository;

    @InjectMocks
    private LoanService loanService;

    UUID userId;
    private Jwt jwt;

    @BeforeEach
    void setUp(){
        userId = UUID.randomUUID();
        jwt = mock(Jwt.class);
    }


    @Nested
    @DisplayName("loan creation tests")
    class LoanCreation {
        private UUID accountId;
        private User user;
        private Account account;

        @BeforeEach
        void setUp(){
            accountId =  UUID.randomUUID();
            user = User.builder().id(userId).build();
            account = Account.builder().id(accountId).user(user).isDeleted(false).build();
        }


        @Test
        void givenValidLoanRequest_whenCreateLoan_thenReturnLoan(){
            LoanRequest request = new LoanRequest(
                    accountId,
                    "Car Loan",
                    new BigDecimal("20000"),
                    new BigDecimal("6.5"),
                    36,
                    LocalDate.of(2026, 1, 1)
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(accountRepository.findByIdAndUserIdAndIsDeletedFalse(accountId, userId))
                    .willReturn(Optional.of(account));

            try(MockedStatic<UserUtility> mockedUserUtility = mockStatic(UserUtility.class)){
                mockedUserUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                LoanResponse response = loanService.createLoan(request, jwt);

                ArgumentCaptor<Loan> capturedLoanArgument = ArgumentCaptor.forClass(Loan.class);
                verify(loanRepository).save(capturedLoanArgument.capture());
                Loan savedLoan = capturedLoanArgument.getValue();

                assertThat(savedLoan.getUser()).isEqualTo(user);
                assertThat(savedLoan.getAccount()).isEqualTo(account);
                assertThat(savedLoan.getLoanName()).isEqualTo("Car Loan");
                assertThat(savedLoan.getPrincipal()).isEqualByComparingTo("20000");
                assertThat(savedLoan.getAnnualRate()).isEqualByComparingTo("6.5");
                assertThat(savedLoan.getTermMonths()).isEqualTo(36);
                assertThat(savedLoan.getMaturityDate()).isEqualTo(LocalDate.of(2029, 1, 1));
                assertThat(savedLoan.getMonthlyPayment()).isPositive();

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<LoanSchedule>> capturedLoanScheduleList = ArgumentCaptor.forClass(List.class);
                verify(loanScheduleRepository).saveAll(capturedLoanScheduleList.capture());
                assertThat(capturedLoanScheduleList.getValue()).hasSize(36);


                assertThat(response).isNotNull();
                assertThat(response.loanName()).isEqualTo("Car Loan");
                assertThat(response.accountId()).isEqualTo(accountId);
            }
        }

        @Test
        void givenInvalidAccountId_whenCreateLoan_thenThrowResourceNotFoundException() {
            LoanRequest request = new LoanRequest(accountId,
                    "Car Loan",
                    new BigDecimal("20000"),
                    new BigDecimal("6.5"),
                    36,
                    LocalDate.of(2026, 1, 1));

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(accountRepository.findByIdAndUserIdAndIsDeletedFalse(accountId, userId))
                    .willReturn(Optional.empty());

            try (MockedStatic<UserUtility> mockedUserUtility = mockStatic(UserUtility.class)) {
                mockedUserUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                assertThatThrownBy(() -> loanService.createLoan(request, jwt))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessage("Account doesn't exist");

                verify(loanRepository, never()).save(any());
                verifyNoInteractions(loanScheduleRepository);
            }

        }
    }


    @Nested
    @DisplayName("loan retrieval tests")
    class LoanRetrieval {

        private UUID loanId;
        private Pageable pageable;

        @BeforeEach
        void setUp() {
            loanId = UUID.randomUUID();
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("getUserLoans returns a paged response scoped to the authenticated user")
        void givenValidCredentials_whenGetUserLoans_thenReturnPagedResult() {
            Loan loan = mock(Loan.class);
            Page<Loan> loanPage = new PageImpl<>(List.of(loan));
            PagedLoanResponse pagedLoanResponse = mock(PagedLoanResponse.class);

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByUserIdAndIsDeletedIsFalse(userId, pageable))
                        .willReturn(loanPage);

                try (MockedStatic<PagedLoanResponse> pagedLoanResponseMockedStatic = mockStatic(PagedLoanResponse.class)) {
                    pagedLoanResponseMockedStatic.when(() -> PagedLoanResponse.of(loanPage)).thenReturn(pagedLoanResponse);

                    PagedLoanResponse response = loanService.getUserLoans(pageable, jwt);

                    assertThat(response).isNotNull();
                    verify(loanRepository).findByUserIdAndIsDeletedIsFalse(userId, pageable);
                }

            }
        }

        @Test
        @DisplayName("getUserLoans returns an empty page when the user has no loans")
        void givenValidCredentials_whenGetUserLoans_thenReturnEmptyPage() {
            Page<Loan> emptyPage = new PageImpl<>(List.of());
            PagedLoanResponse pagedLoanResponse = mock(PagedLoanResponse.class);

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByUserIdAndIsDeletedIsFalse(userId, pageable))
                        .willReturn(emptyPage);

                try (MockedStatic<PagedLoanResponse> pagedLoanResponseMockedStatic = mockStatic(PagedLoanResponse.class)) {
                    pagedLoanResponseMockedStatic.when(() -> PagedLoanResponse.of(emptyPage)).thenReturn(pagedLoanResponse);

                    PagedLoanResponse response = loanService.getUserLoans(pageable, jwt);
                    assertThat(response).isNotNull();
                    verify(loanRepository).findByUserIdAndIsDeletedIsFalse(userId, pageable);

                }
            }

        }

        @Test
        @DisplayName("getLoanById returns the loan when it exists and belongs to the user")
        void givenValidLoanId_whenGetLoanById_thenReturnLoan() {
            Loan loan = mock(Loan.class);
            LoanResponse loanResponse = mock(LoanResponse.class);

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);
                when(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .thenReturn(Optional.of(loan));

                try(MockedStatic<LoanResponse> loanResponseMockedStatic = mockStatic(LoanResponse.class)){
                    loanResponseMockedStatic.when(() -> LoanResponse.of(loan)).thenReturn(loanResponse);

                    LoanResponse response = loanService.getLoanById(loanId, jwt);

                    assertThat(response).isNotNull();
                    verify(loanRepository).findByIdAndUserIdAndIsDeletedFalse(loanId, userId);
                }

            }
        }

        @Test
        @DisplayName("getLoanById throws ResourceNotFoundException when the loan does not exist for the user")
        void givenInvalidLoanId_whenGetLoanById_thenThrowResourceNotFoundException() {
            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);
                when(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> loanService.getLoanById(loanId, jwt))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessage("Loan does not exist");

                verify(loanRepository).findByIdAndUserIdAndIsDeletedFalse(loanId, userId);

            }
        }

        @Test
        @DisplayName("getLoanById does not leak a loan that belongs to a different user")
        void givenAnotherUserLoanId_whenGetLoanById_thenThrowResourceNotFoundException() {
            UUID someoneElsesLoanId = UUID.randomUUID();
            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                // Repository query is scoped by userId, so a loan owned by someone else
                // simply won't match — this locks in that the query stays scoped.
                when(loanRepository.findByIdAndUserIdAndIsDeletedFalse(someoneElsesLoanId, userId))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> loanService.getLoanById(someoneElsesLoanId, jwt))
                        .isInstanceOf(ResourceNotFoundException.class);

            }
        }

    }

    @Nested
    @DisplayName("compare loan tests")
    class CompareLoanTest {
        private UUID loanAId;
        private UUID loanBId;

        @BeforeEach
        void SetUp() {
            loanAId = UUID.randomUUID();
            loanBId = UUID.randomUUID();
        }

        private Loan createLoan(UUID id, String loanName, BigDecimal principal,
                                BigDecimal monthlyPayment, int termMonths, BigDecimal annualRate) {
            return Loan.builder()
                    .id(id)
                    .loanName(loanName)
                    .principal(principal)
                    .monthlyPayment(monthlyPayment)
                    .termMonths(termMonths)
                    .annualRate(annualRate)
                    .build();
        }

        @Test
        @DisplayName("compareLoan names loanA as lower cost when its total payable amount is smaller")
        void givenSmallerLoanACost_whenCompareLoan_thenReturnLoanComparisonResponse(){
            Loan loanA = createLoan(loanAId, "Loan A", new BigDecimal("10000"), new BigDecimal("300"), 36, new BigDecimal("0.05"));
            Loan loanB = createLoan(loanBId, "Loan B", new BigDecimal("15000"), new BigDecimal("500"), 36, new BigDecimal("0.06"));

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);
                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanAId, userId)).willReturn(Optional.of(loanA));
                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanBId, userId)).willReturn(Optional.of(loanB));

                LoanComparisonResponse response = loanService.compareLoan(loanAId, loanBId, jwt);

                assertThat(response.interestDifference()).isEqualByComparingTo("2200.00");
                assertThat(response.monthlyPaymentDifference()).isEqualByComparingTo("200.00");
                assertThat(response.loanWithLowerCost()).isEqualTo("Loan A");
            }

        }

        @Test
        @DisplayName("compareLoan names loanB as lower cost when its total payable amount is smaller")
        void givenSmallerLoanBCost_whenCompareLoan_thenReturnLoanComparisonResponse() {
            Loan loanA = createLoan(loanAId, "Loan A", new BigDecimal("5000"), new BigDecimal("1000"), 12, new BigDecimal("0.08"));
            Loan loanB = createLoan(loanBId, "Loan B", new BigDecimal("5000"), new BigDecimal("500"), 12, new BigDecimal("0.04"));

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);
                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanAId, userId)).willReturn(Optional.of(loanA));
                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanBId, userId)).willReturn(Optional.of(loanB));

                LoanComparisonResponse response = loanService.compareLoan(loanAId, loanBId, jwt);

                assertThat(response.interestDifference()).isEqualByComparingTo("6000.00");
                assertThat(response.monthlyPaymentDifference()).isEqualByComparingTo("500.00");
                assertThat(response.loanWithLowerCost()).isEqualTo("Loan B");

            }
        }

        @Test
        @DisplayName("compareLoan reports equal cost when both loans have the same total payable amount")
        void givenEqualLoanAmount_whenCompareLoan_thenReturnLoanComparisonResponse(){
            Loan loanA = createLoan(loanAId, "Loan A", new BigDecimal("8000"), new BigDecimal("400"), 24, new BigDecimal("0.05"));
            Loan loanB = createLoan(loanBId, "Loan B", new BigDecimal("8500"), new BigDecimal("480"), 20, new BigDecimal("0.05"));

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);
                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanAId, userId)).willReturn(Optional.of(loanA));
                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanBId, userId)).willReturn(Optional.of(loanB));

                LoanComparisonResponse response = loanService.compareLoan(loanAId, loanBId, jwt);

                assertThat(response.interestDifference()).isEqualByComparingTo("500.00");
                assertThat(response.monthlyPaymentDifference()).isEqualByComparingTo("80.00");
                assertThat(response.loanWithLowerCost()).isEqualTo("Both loans have equal payable amount");
            }

        }

        @Test
        @DisplayName("compareLoan throws ResourceNotFoundException when loanB does not exist for the user")
        void givenNonExistentLoanB_whenCompareLoan_thenThrowResourceNotFoundException(){
            Loan loanA = createLoan(loanAId, "Loan A", new BigDecimal("10000"), new BigDecimal("300"), 36, new BigDecimal("0.05"));

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);
                when(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanAId, userId)).thenReturn(Optional.of(loanA));
                when(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanBId, userId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> loanService.compareLoan(loanAId, loanBId, jwt)).hasMessageContaining(loanBId.toString());
                assertThatThrownBy(() -> loanService.compareLoan(loanAId, loanBId, jwt))
                        .isInstanceOf(ResourceNotFoundException.class);
            }

        }
    }

    @Nested
    @DisplayName("simulate payment tests")
    class PaymentSimulation {

        private UUID loanId;
        private Loan loan;
        private LocalDate disbursedAt;

        @BeforeEach
        void setUp() {
            loanId = UUID.randomUUID();
            disbursedAt = LocalDate.of(2026, 1, 1);
            loan = Loan.builder()
                    .id(loanId)
                    .loanName("Personal Loan")
                    .principal(new BigDecimal("6000.0000"))
                    .annualRate(new BigDecimal("12.0"))
                    .termMonths(6)
                    .monthlyPayment(new BigDecimal("1000.0000"))
                    .disbursedAt(disbursedAt)
                    .maturityDate(disbursedAt.plusMonths(6))
                    .build();
        }

        private LoanSchedule createSchedule(int paymentNumber, BigDecimal paymentAmount,
                                            BigDecimal principalPortion, BigDecimal interestPortion,
                                            BigDecimal remainingBalance, LoanScheduleStatus status) {
            return LoanSchedule.builder()
                    .id(UUID.randomUUID())
                    .loan(loan)
                    .paymentNumber(paymentNumber)
                    .paymentAmount(paymentAmount)
                    .principalPortion(principalPortion)
                    .interestPortion(interestPortion)
                    .remainingBalance(remainingBalance)
                    .dueDate(disbursedAt.plusMonths(paymentNumber))
                    .status(status)
                    .build();
        }

        private List<LoanSchedule> buildOriginalSchedule() {
            List<LoanSchedule> schedules = new ArrayList<>();
            schedules.add(createSchedule(1, new BigDecimal("1000.0000"), new BigDecimal("940.0000"), new BigDecimal("60.0000"), new BigDecimal("5060.0000"), LoanScheduleStatus.PENDING));
            schedules.add(createSchedule(2, new BigDecimal("1000.0000"), new BigDecimal("950.0000"), new BigDecimal("50.0000"), new BigDecimal("4110.0000"), LoanScheduleStatus.PENDING));
            schedules.add(createSchedule(3, new BigDecimal("1000.0000"), new BigDecimal("960.0000"), new BigDecimal("40.0000"), new BigDecimal("3150.0000"), LoanScheduleStatus.PENDING));
            schedules.add(createSchedule(4, new BigDecimal("1000.0000"), new BigDecimal("970.0000"), new BigDecimal("30.0000"), new BigDecimal("2180.0000"), LoanScheduleStatus.PENDING));
            schedules.add(createSchedule(5, new BigDecimal("1000.0000"), new BigDecimal("980.0000"), new BigDecimal("20.0000"), new BigDecimal("1200.0000"), LoanScheduleStatus.PENDING));
            schedules.add(createSchedule(6, new BigDecimal("1000.0000"), new BigDecimal("990.0000"), new BigDecimal("10.0000"), BigDecimal.ZERO, LoanScheduleStatus.PENDING));
            return schedules;
        }

        @Test
        @DisplayName("simulatePayment calculates shortened schedule and interest saved on partial extra payment")
        void givenValidRequestWithPartialExtraPayment_whenSimulatePayment_thenReturnSimulatedScheduleAndSavings() {
            int targetPaymentNumber = 1;
            BigDecimal extraAmount = new BigDecimal("2000.0000");

            List<LoanSchedule> originalSchedules = buildOriginalSchedule();
            LoanSchedule targetRow = originalSchedules.getFirst(); // paymentNumber 1, remainingBalance = 5060.0000

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.of(loan));
                given(loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, targetPaymentNumber))
                        .willReturn(Optional.of(targetRow));
                given(loanScheduleRepository.findByLoanId(loanId))
                        .willReturn(originalSchedules);

                LoanSimulationResponse response = loanService.simulatePayment(loanId, targetPaymentNumber, extraAmount, jwt);

                assertThat(response).isNotNull();
                // Original remaining balance was 5060 - 2000 extra = 3060.
                // At 1% monthly rate and 1000 monthly payment:
                // Month 2: interest = 30.60, principal = 969.40, remaining = 2090.60
                // Month 3: interest = 20.906, principal = 979.094, remaining = 1111.5060
                // Month 4: interest = 11.1151, principal = 988.8849, remaining = 122.6211
                // Month 5: interest = 1.2262, principal = 122.6211 (final payoff), actualPayment = 123.8473, remaining = 0
                // Simulated schedule has 4 rows (payments 2, 3, 4, 5)
                assertThat(response.simulatedSchedule()).hasSize(4);
                assertThat(response.monthsSaved()).isEqualTo(1); // 5 original remaining - 4 simulated = 1 month saved
                assertThat(response.interestSaved()).isPositive();

                // Verify first simulated row
                assertThat(response.simulatedSchedule().getFirst().paymentNumber()).isEqualTo(2);
                assertThat(response.simulatedSchedule().getFirst().dueDate()).isEqualTo(disbursedAt.plusMonths(2));
                assertThat(response.simulatedSchedule().getFirst().loanScheduleStatus()).isEqualTo(LoanScheduleStatus.PENDING);

                // Verify last simulated row pays off the remaining balance
                assertThat(response.simulatedSchedule().getLast().paymentNumber()).isEqualTo(5);
                assertThat(response.simulatedSchedule().getLast().remainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            }
        }

        @Test
        @DisplayName("simulatePayment returns full payoff response when extra amount covers exact remaining balance")
        void givenExtraAmountEqualsRemainingBalance_whenSimulatePayment_thenReturnFullPayoffResponse() {
            int targetPaymentNumber = 1;
            List<LoanSchedule> originalSchedules = buildOriginalSchedule();
            LoanSchedule targetRow = originalSchedules.getFirst(); // remainingBalance = 5060.0000
            BigDecimal extraAmount = new BigDecimal("5060.0000");

            // Original remaining interest for payments 2..6: 50 + 40 + 30 + 20 + 10 = 150.0000
            BigDecimal expectedInterestSaved = new BigDecimal("150.0000");

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.of(loan));
                given(loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, targetPaymentNumber))
                        .willReturn(Optional.of(targetRow));
                given(loanScheduleRepository.findByLoanId(loanId))
                        .willReturn(originalSchedules);

                LoanSimulationResponse response = loanService.simulatePayment(loanId, targetPaymentNumber, extraAmount, jwt);

                assertThat(response).isNotNull();
                assertThat(response.simulatedSchedule()).isEmpty();
                assertThat(response.interestSaved()).isEqualByComparingTo(expectedInterestSaved);
                assertThat(response.monthsSaved()).isEqualTo(5);
            }
        }

        @Test
        @DisplayName("simulatePayment returns full payoff response when extra amount exceeds remaining balance")
        void givenExtraAmountExceedsRemainingBalance_whenSimulatePayment_thenReturnFullPayoffResponse() {
            int targetPaymentNumber = 1;
            List<LoanSchedule> originalSchedules = buildOriginalSchedule();
            LoanSchedule targetRow = originalSchedules.getFirst();
            BigDecimal extraAmount = new BigDecimal("6000.0000"); // Greater than 5060.0000

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.of(loan));
                given(loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, targetPaymentNumber))
                        .willReturn(Optional.of(targetRow));
                given(loanScheduleRepository.findByLoanId(loanId))
                        .willReturn(originalSchedules);

                LoanSimulationResponse response = loanService.simulatePayment(loanId, targetPaymentNumber, extraAmount, jwt);

                assertThat(response).isNotNull();
                assertThat(response.simulatedSchedule()).isEmpty();
                assertThat(response.interestSaved()).isEqualByComparingTo("150.0000");
                assertThat(response.monthsSaved()).isEqualTo(5);
            }
        }

        @Test
        @DisplayName("simulatePayment throws RuntimeException when loan does not exist")
        void givenNonExistentLoan_whenSimulatePayment_thenThrowRuntimeException() {
            int paymentNumber = 1;
            BigDecimal extraAmount = new BigDecimal("500.0000");

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() -> loanService.simulatePayment(loanId, paymentNumber, extraAmount, jwt))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Loan not found");

                verifyNoInteractions(loanScheduleRepository);
            }
        }

        @Test
        @DisplayName("simulatePayment throws BadRequestException when extra amount is zero")
        void givenZeroExtraAmount_whenSimulatePayment_thenThrowBadRequestException() {
            int paymentNumber = 1;
            BigDecimal extraAmount = BigDecimal.ZERO;

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.of(loan));

                assertThatThrownBy(() -> loanService.simulatePayment(loanId, paymentNumber, extraAmount, jwt))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessage("Extra amount must be greater than zero");

                verifyNoInteractions(loanScheduleRepository);
            }
        }

        @Test
        @DisplayName("simulatePayment throws BadRequestException when extra amount is negative")
        void givenNegativeExtraAmount_whenSimulatePayment_thenThrowBadRequestException() {
            int paymentNumber = 1;
            BigDecimal extraAmount = new BigDecimal("-100.0000");

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.of(loan));

                assertThatThrownBy(() -> loanService.simulatePayment(loanId, paymentNumber, extraAmount, jwt))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessage("Extra amount must be greater than zero");

                verifyNoInteractions(loanScheduleRepository);
            }
        }

        @Test
        @DisplayName("simulatePayment throws ResourceNotFoundException when payment number is not found in schedule")
        void givenNonExistentPaymentNumber_whenSimulatePayment_thenThrowResourceNotFoundException() {
            int paymentNumber = 99;
            BigDecimal extraAmount = new BigDecimal("500.0000");

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.of(loan));
                given(loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, paymentNumber))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() -> loanService.simulatePayment(loanId, paymentNumber, extraAmount, jwt))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessage("Payment number not found in schedule");
            }
        }

        @Test
        @DisplayName("simulatePayment throws BadRequestException when target installment is already paid")
        void givenAlreadyPaidInstallment_whenSimulatePayment_thenThrowBadRequestException() {
            int paymentNumber = 1;
            BigDecimal extraAmount = new BigDecimal("500.0000");
            LoanSchedule paidRow = createSchedule(1, new BigDecimal("1000.0000"), new BigDecimal("940.0000"),
                    new BigDecimal("60.0000"), new BigDecimal("5060.0000"), LoanScheduleStatus.PAID);

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.of(loan));
                given(loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, paymentNumber))
                        .willReturn(Optional.of(paidRow));

                assertThatThrownBy(() -> loanService.simulatePayment(loanId, paymentNumber, extraAmount, jwt))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessage("Cannot simulate against an installment that is already paid");
            }
        }

        @Test
        @DisplayName("simulatePayment handles zero annual rate loan correctly")
        void givenZeroAnnualRateLoan_whenSimulatePayment_thenSimulateWithoutInterest() {
            int targetPaymentNumber = 1;
            BigDecimal extraAmount = new BigDecimal("1000.0000");

            Loan zeroInterestLoan = Loan.builder()
                    .id(loanId)
                    .loanName("Zero Interest Loan")
                    .principal(new BigDecimal("3000.0000"))
                    .annualRate(BigDecimal.ZERO)
                    .termMonths(3)
                    .monthlyPayment(new BigDecimal("1000.0000"))
                    .disbursedAt(disbursedAt)
                    .maturityDate(disbursedAt.plusMonths(3))
                    .build();

            List<LoanSchedule> schedules = List.of(
                    createSchedule(1, new BigDecimal("1000.0000"), new BigDecimal("1000.0000"), BigDecimal.ZERO, new BigDecimal("2000.0000"), LoanScheduleStatus.PENDING),
                    createSchedule(2, new BigDecimal("1000.0000"), new BigDecimal("1000.0000"), BigDecimal.ZERO, new BigDecimal("1000.0000"), LoanScheduleStatus.PENDING),
                    createSchedule(3, new BigDecimal("1000.0000"), new BigDecimal("1000.0000"), BigDecimal.ZERO, BigDecimal.ZERO, LoanScheduleStatus.PENDING)
            );

            try (MockedStatic<UserUtility> userUtility = mockStatic(UserUtility.class)) {
                userUtility.when(() -> UserUtility.getUserId(jwt)).thenReturn(userId);

                given(loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId))
                        .willReturn(Optional.of(zeroInterestLoan));
                given(loanScheduleRepository.findByLoanIdAndPaymentNumber(loanId, targetPaymentNumber))
                        .willReturn(Optional.of(schedules.getFirst()));
                given(loanScheduleRepository.findByLoanId(loanId))
                        .willReturn(schedules);

                LoanSimulationResponse response = loanService.simulatePayment(loanId, targetPaymentNumber, extraAmount, jwt);

                assertThat(response).isNotNull();
                // 2000 - 1000 extra = 1000 remaining. 1 payment of 1000 pays off remaining balance.
                assertThat(response.simulatedSchedule()).hasSize(1);
                assertThat(response.monthsSaved()).isEqualTo(1); // 2 original remaining - 1 simulated = 1 month saved
                assertThat(response.interestSaved()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(response.simulatedSchedule().getFirst().paymentAmount()).isEqualByComparingTo("1000.0000");
                assertThat(response.simulatedSchedule().getFirst().remainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            }
        }
    }
}
