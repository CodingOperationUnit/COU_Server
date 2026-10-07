package com.couserver.account.repository;

import com.couserver.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    boolean existsByAccountLoginId(String accountLoginId);
    Optional<Account> findByAccountLoginId(String accountLoginId);
}
