package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.aspect.Auditable;
import com.lancea.personal_finance_loan_api.dto.request.AccountRequest;
import com.lancea.personal_finance_loan_api.dto.request.AccountUpdateRequest;
import com.lancea.personal_finance_loan_api.dto.response.AccountResponse;
import com.lancea.personal_finance_loan_api.entity.Account;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import com.lancea.personal_finance_loan_api.exception.ResourceNotFoundException;
import com.lancea.personal_finance_loan_api.repository.AccountRepository;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import com.lancea.personal_finance_loan_api.utility.UserUtility;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Auditable(action = "ACCOUNT_CREATED", entityType = "ACCOUNT")
    public AccountResponse createAccount(Jwt jwt, AccountRequest accountRequest){
        UUID userId = UserUtility.getUserId(jwt);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found" + userId));


        Account newAccount = Account.builder()
                .user(user)
                .accountName(accountRequest.accountName())
                .accountType(accountRequest.accountType())
                .currency(accountRequest.currency())
                .build();

        Account saved = accountRepository.save(newAccount);

        return  AccountResponse.of(saved);


    }

    public List<AccountResponse> getAllAccounts(Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        return accountRepository.findByUserIdAndIsDeletedFalse(userId).stream().map( account -> AccountResponse.of(account))
                .collect(Collectors.toList());
    }


    public AccountResponse getAccountById(UUID accountId, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(accountId, userId)
                .orElseThrow( () -> new ResourceNotFoundException("Account doesn't exist or has been deleted"));

        return AccountResponse.of(account);
    }

    @Transactional
    @Auditable(action = "ACCOUNT_UPDATED", entityType = "ACCOUNT")
    public AccountResponse updateAccount(UUID accountId, AccountUpdateRequest updateRequest,
                                         Jwt jwt){

        UUID userId = UserUtility.getUserId(jwt);

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(accountId,userId)
                .orElseThrow( ()-> new ResourceNotFoundException("Account doesn't exist or has been deleted"));

        account.setAccountName(updateRequest.accountName());
        account.setAccountType(updateRequest.accountType());

        accountRepository.save(account);

        return AccountResponse.of(account);

    }

    @Auditable(action = "ACCOUNT_CLOSED", entityType = "ACCOUNT")
    public AccountResponse closeAccount(UUID accountId, Jwt jwt)  {

        UUID userId = UserUtility.getUserId(jwt);

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(accountId, userId)
                .orElseThrow( () -> new ResourceNotFoundException("Account doesn't exist or has been deleted"));

        if(account.getBalance().compareTo(BigDecimal.ZERO) > 0) throw new BadRequestException
                ("Cannot close an account with remaining balance of" + account.getBalance());


        account.setIsActive(false);

        accountRepository.save(account);

        return AccountResponse.of(account);
    }

    @Transactional
    @Auditable(action = "ACCOUNT_DELETED", entityType = "ACCOUNT")
    public void deleteAccount(UUID accountId, Jwt jwt){
        UUID userId = UserUtility.getUserId(jwt);

        Account account = accountRepository.findByIdAndUserIdAndIsDeletedFalse(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account doesn't exist or has been deleted"));

        account.setIsDeleted(true);
        account.setDeletedAt(Instant.now());

        accountRepository.save(account);
    }

}
