package com.carddemo.billpay.repo;

import com.carddemo.billpay.domain.CardXref;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!jdbc")
public class InMemoryCardXrefRepository implements CardXrefRepository {

    private final ConcurrentHashMap<String, CardXref> byAccount = new ConcurrentHashMap<>();

    @Override
    public Optional<CardXref> findByAccountId(String accountId) {
        return Optional.ofNullable(byAccount.get(accountId));
    }

    public void seed(CardXref xref) {
        byAccount.put(xref.accountId(), xref);
    }

    public void clear() {
        byAccount.clear();
    }
}
