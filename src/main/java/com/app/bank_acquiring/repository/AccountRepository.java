package com.app.bank_acquiring.repository;

import com.app.bank_acquiring.domain.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByUsername(String username);
    void deleteById(Long id);
}
