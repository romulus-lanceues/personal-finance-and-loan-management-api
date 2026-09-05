package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.LoanRequest;
import com.lancea.personal_finance_loan_api.dto.response.LoanResponse;
import com.lancea.personal_finance_loan_api.dto.response.PagedLoanResponse;
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
    UserRepository userRepository;

    @Mock
    AccountRepository accountRepository;

    @Mock
    LoanRepository loanRepository;

    @Mock
    LoanScheduleRepository loanScheduleRepository;

    @InjectMocks
    LoanService loanService;

    UUID userId;
    Jwt jwt;

    @BeforeEach
    void setUp(){
        userId = UUID.randomUUID();
        jwt = mock(Jwt.class);
    }


    @Nested
    @DisplayName("loan creation tests")
    class LoanCreation {
        UUID accountId;
        User user;
        Account account;

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
        void whenAnotherUserLoanId_whenGetLoanById_thenThrowResourceNotFoundException() {
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
}
