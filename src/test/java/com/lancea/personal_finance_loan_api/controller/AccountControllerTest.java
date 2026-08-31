package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.config.AuthenticationPrincipalTestConfig;
import com.lancea.personal_finance_loan_api.config.TestSecurityConfig;
import com.lancea.personal_finance_loan_api.dto.request.AccountRequest;
import com.lancea.personal_finance_loan_api.dto.request.AccountUpdateRequest;
import com.lancea.personal_finance_loan_api.dto.response.AccountResponse;
import com.lancea.personal_finance_loan_api.enums.AccountType;
import com.lancea.personal_finance_loan_api.enums.Currency;
import com.lancea.personal_finance_loan_api.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({AuthenticationPrincipalTestConfig.class, TestSecurityConfig.class})
public class AccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AccountService accountService;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    CacheManager cacheManager;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("account creation test")
    class AccountCreation {

        AccountRequest accountRequest;

        @BeforeEach
        void setUp(){
            accountRequest = new AccountRequest("Savings Account", AccountType.SAVINGS, Currency.PHP);
        }

        @Test
        void givenAccountRequest_whenCreateAccount_thenReturnCreatedAccountResponse() throws Exception {

            AccountResponse testAccountResponse = new AccountResponse(
                    UUID.fromString("d44ce33e-08ed-4b8d-9c9c-fa738c72215e"),
                    accountRequest.accountName(),
                    accountRequest.accountType(),
                    BigDecimal.ZERO,
                    accountRequest.currency(),
                    true,
                    Instant.now(),
                    null);

            given(accountService.createAccount(any(Jwt.class), any(AccountRequest.class))).willReturn(testAccountResponse);

            mockMvc.perform(post("/api/v1/accounts")
                            .with(jwt().jwt(j -> j.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/v1/accounts/d44ce33e-08ed-4b8d-9c9c-fa738c72215e"))
                    .andExpect(jsonPath("$.id").value("d44ce33e-08ed-4b8d-9c9c-fa738c72215e"))
                    .andExpect(jsonPath("$.accountName").value("Savings Account"))
                    .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                    .andExpect(jsonPath("$.balance").value(0))
                    .andExpect(jsonPath("$.currency").value("PHP"));
        }


        @Test
        void givenNoJwtToken_whenCreateAccount_thenThrowUnauthorized() throws Exception {

            mockMvc.perform(post("/api/v1/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountRequest)))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(accountService);

        }
    }

    @Nested
    @DisplayName("invalid account request test")
    class InvalidRequest {

        @Test
        void givenBlankAccountName_whenCreateAccount_thenThrowBadRequest() throws Exception {

            String invalidJson = """
                {"accountName" : "", "accountType": "SAVINGS", "currency": "PHP"}
                """;

            mockMvc.perform(post("/api/v1/accounts")
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6" )))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(accountService);
        }
    }


    @Nested
    @DisplayName("get all user accounts tests")
    class GetAllAccounts {

        @Test
        void givenJwtToken_whenGetAllAccounts_thenReturnAccountList() throws Exception{

            List<AccountResponse> accountResponses = List.of(
                    new AccountResponse(
                            UUID.fromString("11111111-1111-1111-1111-111111111111"),
                            "Emergency Fund",
                            AccountType.SAVINGS,
                            BigDecimal.valueOf(25000),
                            Currency.PHP,
                            true,
                            Instant.now(),
                            null),
                    new AccountResponse(
                            UUID.fromString("22222222-2222-2222-2222-222222222222"),
                            "Payroll Account",
                            AccountType.CHECKING,
                            BigDecimal.valueOf(15000),
                            Currency.PHP,
                            true,
                            Instant.now(),
                            null),
                    new AccountResponse(
                            UUID.fromString("33333333-3333-3333-3333-333333333333"),
                            "Cash Wallet",
                            AccountType.CASH,
                            BigDecimal.valueOf(5000),
                            Currency.PHP,
                            true,
                            Instant.now(),
                            null),
                    new AccountResponse(
                            UUID.fromString("44444444-4444-4444-4444-444444444444"),
                            "USD Savings",
                            AccountType.SAVINGS,
                            BigDecimal.valueOf(1000),
                            Currency.USD,
                            true,
                            Instant.now(),
                            null),
                    new AccountResponse(
                            UUID.fromString("55555555-5555-5555-5555-555555555555"),
                            "Travel Fund",
                            AccountType.SAVINGS,
                            BigDecimal.valueOf(75000),
                            Currency.JPY,
                            true,
                            Instant.now(),
                            null)
            );


            given(accountService.getAllAccounts(any(Jwt.class))).willReturn(accountResponses);

            mockMvc.perform(get("/api/v1/accounts")
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(5))
                    .andExpect(jsonPath("$[0].accountName").value("Emergency Fund"))
                    .andExpect(jsonPath("$[3].currency").value("USD"));
        }

        @Test
        void givenNoJwtToken_whenGetAllAccounts_thenThrowUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
            verifyNoInteractions(accountService);
        }

    }


    @Nested
    @DisplayName("get account by id tests")
    class GetAccountByIdTest {

        @Test
        void givenAccountId_whenGetAccountById_thenReturnAccount() throws Exception{
            UUID accountId = UUID.fromString("de8365d5-a29e-489b-92cc-394f833b60d6");

            AccountResponse mockResponse = new AccountResponse(
                    UUID.fromString("44444444-4444-4444-4444-444444444444"),
                    "USD Savings",
                    AccountType.SAVINGS,
                    BigDecimal.valueOf(1000),
                    Currency.USD,
                    true,
                    Instant.now(),
                    null);


            given(accountService.getAccountById(eq(accountId), any(Jwt.class))).willReturn(mockResponse);

            mockMvc.perform(get("/api/v1/accounts/{accountId}", accountId)
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountName").value("USD Savings"))
                    .andExpect(jsonPath("$.currency").value("USD"));

        }

        @Test
        void givenNoJwtToken_whenGetAccountById_thenThrowUnauthorized() throws Exception {

            UUID accountId = UUID.fromString("de8365d5-a29e-489b-92cc-394f833b60d6");

            mockMvc.perform(get("/api/v1/accounts/{accountId}", accountId))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(accountService);
        }

        @Test
        void givenInvalidAccountId_whenGetAccountById_thenThrowBadRequest() throws Exception{
            int accountId = 231782;
            mockMvc.perform(get("/api/v1/accounts/{accountId}", accountId)
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(accountService);

        }
    }

    @Nested
    @DisplayName("update account tests")
    class UpdateAccountTests {

        AccountUpdateRequest accountUpdateRequest;
        UUID accountId;

        @BeforeEach
        void setUp(){
            accountUpdateRequest = new AccountUpdateRequest("Updated name", AccountType.CHECKING);
            accountId = UUID.fromString("de8365d5-a29e-489b-92cc-394f833b60d6");
        }

        @Test
        void givenUpdateRequest_whenUpdateAccount_thenReturnAccountResponse() throws Exception{

            AccountResponse accountResponse = new AccountResponse(
                    UUID.fromString("d44ce33e-08ed-4b8d-9c9c-fa738c72215e"),
                    "Updated name",
                    AccountType.CHECKING,
                    BigDecimal.ZERO,
                    Currency.PHP,
                    true,
                    Instant.now(),
                    null);


            given(accountService.updateAccount(eq(accountId), any(AccountUpdateRequest.class), any(Jwt.class))).willReturn(accountResponse);

            mockMvc.perform(put("/api/v1/accounts/{accountId}", accountId)
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("d44ce33e-08ed-4b8d-9c9c-fa738c72215e"))
                    .andExpect(jsonPath("$.accountName").value("Updated name"))
                    .andExpect(jsonPath("$.accountType").value("CHECKING"))
                    .andExpect(jsonPath("$.balance").value(0));
        }

        @Test
        void givenNoJwtToken_whenUpdateAccount_thenThrowUnauthorized() throws Exception {

            mockMvc.perform(put("/api/v1/accounts/{accountId}", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountUpdateRequest)))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(accountService);
        }

        @Test
        void givenEmptyAccountName_whenUpdateAccount_thenThrowBadRequest() throws Exception {

            String invalidRequest ="""
                {"accountName" : "", "accountType": "SAVINGS"}
                """;

            mockMvc.perform(put("/api/v1/accounts/{accountId}", accountId)
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(accountService);

        }

    }


    @Nested
    @DisplayName("close and delete account tests")
    class CloseAndDeleteAccountTests {

        UUID accountId;

        @BeforeEach
        void setUp(){
            accountId = UUID.fromString("de8365d5-a29e-489b-92cc-394f833b60d6");
        }

        @Test
        void givenAccountId_whenCloseAccount_thenReturnAccountResponse() throws Exception {

            AccountResponse accountResponse = new AccountResponse(
                    UUID.fromString("d44ce33e-08ed-4b8d-9c9c-fa738c72215e"),
                    "Updated name",
                    AccountType.CHECKING,
                    BigDecimal.ZERO,
                    Currency.PHP,
                    false,
                    Instant.now(),
                    null);

            given(accountService.closeAccount(eq(accountId), any(Jwt.class))).willReturn(accountResponse);

            mockMvc.perform(patch("/api/v1/accounts/{accountId}/close", accountId)
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("d44ce33e-08ed-4b8d-9c9c-fa738c72215e"))
                    .andExpect(jsonPath("$.accountName").value("Updated name"))
                    .andExpect(jsonPath("$.accountType").value("CHECKING"));

        }

        @Test
        void givenNoJwtToken_whenCloseAccount_thenReturnAccountResponse() throws Exception {

            mockMvc.perform(patch("/api/v1/accounts/{accountId}/close", accountId))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(accountService);

        }

        @Test
        void givenInvalidAccountId_whenCloseAccount_thenThrowBadRequest() throws Exception {
            int accountId = 231782;

            mockMvc.perform(patch("/api/v1/accounts/{accountId}/close", accountId)
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(accountService);

        }


        @Test
        void givenAccountId_whenDeleteAccount_thenDelete() throws Exception {

            given(accountService.deleteAccount(any(UUID.class), any(Jwt.class))).willReturn(null);

            mockMvc.perform(delete("/api/v1/accounts/{accountId}", accountId)
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        void givenNoJwtToken_whenDeleteAccount_thenThrowUnauthorized() throws Exception {

            mockMvc.perform(delete("/api/v1/accounts/{accountId}", accountId))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(accountService);
        }

        @Test
        void givenInvalidAccountId_whenDeleteAccount_thenThrowBadRequest() throws Exception {
            int accountId = 231782;

            mockMvc.perform(delete("/api/v1/accounts/{accountId}", accountId)
                            .with(jwt().jwt( jwt -> jwt.claim("userId", "2fa302e2-e6ad-4b8f-8675-47f5b97ed8e6"))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(accountService);

        }

    }




}
