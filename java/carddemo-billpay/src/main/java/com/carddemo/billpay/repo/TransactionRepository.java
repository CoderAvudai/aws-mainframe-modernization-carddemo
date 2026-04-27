package com.carddemo.billpay.repo;

import com.carddemo.billpay.domain.Transaction;

import java.util.Optional;

/** Abstraction over VSAM dataset {@code TRANSACT}. */
public interface TransactionRepository {

    /**
     * Equivalent of the
     * {@code STARTBR(HIGH-VALUES) → READPREV → ENDBR} sequence used by R22: it
     * returns the record with the highest {@code TRAN-ID} currently in the
     * file, or {@code Optional.empty()} if the file is empty (COBOL
     * {@code DFHRESP(ENDFILE)}).
     */
    Optional<Transaction> findHighestTransactionId();

    /** Equivalent of {@code EXEC CICS WRITE DATASET(TRANSACT)}. */
    void save(Transaction transaction);

    /** True when a record already exists for the given id (drives DUPKEY). */
    boolean existsById(String transactionId);
}
