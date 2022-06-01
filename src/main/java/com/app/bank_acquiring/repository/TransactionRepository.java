package com.app.bank_acquiring.repository;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findByTerminal(Terminal terminal);
}
