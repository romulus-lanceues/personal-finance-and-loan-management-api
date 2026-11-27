package com.lancea.personal_finance_loan_api.repository;

import com.lancea.personal_finance_loan_api.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdAndIsDeletedFalse(UUID userId);

    Optional<Account> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findForUpdateByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Optional<Account> findByIdAndUserIdAndIsActiveTrueAndIsDeletedFalse(UUID id, UUID userId);

}
