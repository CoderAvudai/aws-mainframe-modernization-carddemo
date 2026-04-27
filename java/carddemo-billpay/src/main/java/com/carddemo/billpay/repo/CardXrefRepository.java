package com.carddemo.billpay.repo;

import com.carddemo.billpay.domain.CardXref;

import java.util.Optional;

/** Abstraction over VSAM alternate index {@code CXACAIX}. */
public interface CardXrefRepository {

    /** Equivalent of {@code EXEC CICS READ DATASET(CXACAIX)} keyed by acct id. */
    Optional<CardXref> findByAccountId(String accountId);
}
