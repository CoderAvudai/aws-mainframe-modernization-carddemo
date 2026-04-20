package com.carddemo.billpay.repo;

import com.carddemo.billpay.domain.Transaction;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@Profile("!jdbc")
public class InMemoryTransactionRepository implements TransactionRepository {

    private final ConcurrentMap<String, Transaction> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Transaction> findHighestTransactionId() {
        return store.values().stream().max(Comparator.comparing(Transaction::transactionId));
    }

    @Override
    public void save(Transaction transaction) {
        store.put(transaction.transactionId(), transaction);
    }

    @Override
    public boolean existsById(String transactionId) {
        return store.containsKey(transactionId);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }
}
