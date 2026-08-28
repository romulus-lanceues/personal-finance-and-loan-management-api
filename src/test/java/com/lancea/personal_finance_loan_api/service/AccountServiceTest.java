package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.AccountRequest;
import com.lancea.personal_finance_loan_api.dto.request.AccountUpdateRequest;
import com.lancea.personal_finance_loan_api.dto.response.AccountResponse;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.enums.AccountType;
import com.lancea.personal_finance_loan_api.enums.Currency;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    AccountService accountService;

    Jwt jwtToken;

    @Nested
    @DisplayName("account creation tests")
    class CreateAccount {
        AccountRequest request;

        @BeforeEach
        void setup(){
            request = new AccountRequest("Test Account", AccountType.CHECKING, Currency.PHP);
            jwtToken = getJwtToken();
        }

        @Test
        void  givenAccountRequestAndJwtToken_whenCreateAccount_thenReturnCreatedAccount(){

            User user = User.builder()
                    .id(UUID.fromString("2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"))
                    .fullName("John Doe").email("testmail@maily.com")
                    .password("finaceandloanmanagementapp")
                    .build();

            Account account = Account.builder()
                    .accountName("Test Account")
                    .accountType(AccountType.CHECKING)
                    .currency(Currency.PHP)
                    .build();

            given(userRepository
                    .findById(UUID.fromString("2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6")))
                    .willReturn(Optional.of(user));

            given(accountRepository.save(any(Account.class))).willReturn(account);

            AccountResponse result = accountService.createAccount(jwtToken, request);

            assertThat(result.accountName()).isEqualTo("Test Account");
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        void givenAccountRequestAndJwtToken_whenCreateAccount_ThenUserNotFound (){

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.createAccount(jwtToken, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found" +  UUID.fromString("2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"));

            verify(accountRepository, never()).save(any());
        }

    }

    @Nested
    @DisplayName("updating account tests")
    class UpdateAccount {

        AccountUpdateRequest request;
        UUID accountId;

        @BeforeEach
        void setUp(){
            jwtToken = getJwtToken();
            request = new AccountUpdateRequest("Savings Account", AccountType.CHECKING);
            accountId = UUID.fromString("13ea2036-1929-482c-bef8-e6162f0647d7");
        }

        @Test
        void givenAccountIdAndRequest_whenUpdateAccount_thenReturnUpdatedAccount(){

            Account outdatedAccount = Account.builder()
                    .accountName("Test Account")
                    .accountType(AccountType.SAVINGS)
                    .build();

            given(accountRepository
                    .findByIdAndUserIdAndIsDeletedFalse(eq(accountId), any(UUID.class)))
                    .willReturn(Optional.of(outdatedAccount));

            AccountResponse result = accountService.updateAccount(accountId, request, jwtToken);

            assertThat(result.accountName()).isEqualTo(request.accountName());
            assertThat(result.accountType()).isEqualTo(request.accountType());
            verify(accountRepository).save(outdatedAccount);

        }

        @Test
        void givenAccountIdAndRequest_whenUpdateAccount_thenThrowResourceNotFound(){

            given(accountRepository.findByIdAndUserIdAndIsDeletedFalse(eq(accountId), any(UUID.class)))
                    .willReturn(Optional.empty());

            assertAll(
                    () -> assertThatThrownBy(() -> accountService.updateAccount(accountId, request, jwtToken))
                        .isInstanceOf(ResourceNotFoundException.class),
                    () -> verify(accountRepository, never()).save(any())
            );
        }

    }

    @Nested
    @DisplayName("closing and deleting accounts tests")
    class CloseDeleteAccount {
        UUID accountId;

        @BeforeEach
        void setup(){
            accountId = UUID.fromString("13ea2036-1929-482c-bef8-e6162f0647d7");
            jwtToken = getJwtToken();

        }

        @Test
        void givenAccountIdAndJwt_whenCloseAccount_thenReturnClosedAccount(){

            Account account = Account.builder()
                    .accountName("Test Account")
                    .accountType(AccountType.SAVINGS)
                    .isActive(true)
                    .build();

            given(accountRepository.findByIdAndUserIdAndIsDeletedFalse(eq(accountId), any(UUID.class))).willReturn(Optional.of(account));

            AccountResponse result = accountService.closeAccount(accountId, jwtToken);

            assertAll(
                    () -> assertThat(result.accountName()).isEqualTo(account.getAccountName()),
                    () -> assertThat(result.isActive()).isEqualTo(false),
                    () -> verify(accountRepository).save(account)
            );
        }

        @Test
        void givenAccountIdAndJwt_whenCloseAccount_thenThrowUserHasRemainingBalance(){

            Account account = Account.builder()
                    .accountName("Test Account")
                    .balance(BigDecimal.valueOf(20000))
                    .accountType(AccountType.SAVINGS)
                    .isActive(true)
                    .build();

            given(accountRepository
                    .findByIdAndUserIdAndIsDeletedFalse(eq(accountId), any(UUID.class)))
                    .willReturn(Optional.of(account));

            assertAll(
                    () -> assertThatThrownBy(() -> accountService.closeAccount(accountId, jwtToken)).isInstanceOf(BadRequestException.class),
                    () -> verify(accountRepository, never()).save(any())
            );
        }

        @Test
        void givenAccountIdAndJwt_whenDeleteAccount_thenReturnDeletedAccount(){

            Account account = Account.builder()
                    .accountName("Test Account")
                    .accountType(AccountType.SAVINGS)
                    .isDeleted(false)
                    .build();

            given(accountRepository
                    .findByIdAndUserIdAndIsDeletedFalse(eq(accountId), any(UUID.class)))
                    .willReturn(Optional.of(account));

            AccountResponse result = accountService.deleteAccount(accountId, jwtToken);

            assertAll(
                    () -> assertThat(account.getIsDeleted()).isEqualTo(true),
                    () -> assertThat(result.accountName()).isEqualTo(account.getAccountName()),
                    () -> assertThat(account.getDeletedAt()).isNotNull(),
                    () -> verify(accountRepository).save(any(Account.class))
            );

        }

        @Test
        void givenAccountIdAndJwt_whenDeleteAccount_thenThrowAccountCannotBeFound() {
            given(accountRepository
                    .findByIdAndUserIdAndIsDeletedFalse(eq(accountId), any(UUID.class)))
                    .willReturn(Optional.empty());

            assertAll(
                    () -> assertThatThrownBy(() -> accountService.deleteAccount(accountId, jwtToken))
                            .isInstanceOf(ResourceNotFoundException.class),
                    () -> verify(accountRepository, never()).save(any())
            );
        }
    }



    @Test
    void givenJwtTokenWithNoUserIdClaim_whenAnyAccountServiceMethod_thenThrowBadRequestException(){

        AccountRequest request = new AccountRequest("Test Account", AccountType.CHECKING, Currency.PHP);

        Jwt jwtToken = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("test", "123456789")
                .build();

        assertThrows(BadRequestException.class, () -> accountService.createAccount(jwtToken, request));
    }


    private Jwt getJwtToken(){
        return Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6")
                .build();
    }
}
