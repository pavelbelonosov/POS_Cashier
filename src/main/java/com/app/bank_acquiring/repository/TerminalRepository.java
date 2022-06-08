package com.app.bank_acquiring.repository;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminalRepository extends JpaRepository<Terminal,Long> {
    Terminal findByTid(int tid);
    List<Terminal> findByAccount(Account account);
}
