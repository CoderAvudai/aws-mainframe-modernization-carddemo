package com.aws.carddemo.billpay.repository;

import com.aws.carddemo.billpay.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CardXrefRepository extends JpaRepository<CardXref, String> {
    Optional<CardXref> findByAcctId(String acctId);
}
