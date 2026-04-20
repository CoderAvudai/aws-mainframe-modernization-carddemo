package com.carddemo.billpay.repo;

import com.carddemo.billpay.domain.Account;

import java.util.Optional;

/** Abstraction over VSAM dataset {@code ACCTDAT} (READ UPDATE / REWRITE). */
public interface AccountRepository {

    /** Equivalent of {@code EXEC CICS READ DATASET(ACCTDAT) UPDATE}. */
    Optional<Account> findById(String accountId);

    /** Equivalent of {@code EXEC CICS REWRITE DATASET(ACCTDAT)}. */
    void update(Account account);
}
