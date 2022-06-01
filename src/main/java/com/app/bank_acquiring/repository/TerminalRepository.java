package com.app.bank_acquiring.repository;

import com.app.bank_acquiring.domain.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalRepository extends JpaRepository<Terminal,Long> {
    Terminal findByTid(int tid);
}
