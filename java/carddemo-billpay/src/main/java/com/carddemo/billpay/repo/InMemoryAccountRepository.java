package com.carddemo.billpay.repo;

import com.carddemo.billpay.domain.Account;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation, suitable for local development and for the
 * JUnit 5 test suite. Production deployments would swap this for a JDBC or
 * DB2-backed repository via an alternate Spring profile.
 */
@Repository
@Profile("!jdbc")
public class InMemoryAccountRepository implements AccountRepository {

    private final ConcurrentHashMap<String, Account> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(String accountId) {
        return Optional.ofNullable(store.get(accountId));
    }

    @Override
    public void update(Account account) {
        store.put(account.accountId(), account);
    }

    /** Test / bootstrap helper — not part of the repository contract. */
    public void seed(Account account) {
        store.put(account.accountId(), account);
    }

    public void clear() {
        store.clear();
    }
}
