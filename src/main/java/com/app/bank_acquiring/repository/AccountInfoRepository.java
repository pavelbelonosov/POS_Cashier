package com.app.bank_acquiring.repository;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountInfoRepository extends JpaRepository<AccountInfo,Long> {
    AccountInfo findByAccount(Account account);
}
