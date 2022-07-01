package com.app.bank_acquiring.repository;

import com.app.bank_acquiring.domain.SalesCounter;
import com.app.bank_acquiring.domain.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesCounterRepository extends JpaRepository<SalesCounter,Long> {

    SalesCounter findByTerminalTid(String terminalTid);
}
