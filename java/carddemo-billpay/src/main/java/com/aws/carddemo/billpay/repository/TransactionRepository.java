package com.aws.carddemo.billpay.repository;

import com.aws.carddemo.billpay.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT t FROM Transaction t ORDER BY t.tranId DESC LIMIT 1")
    Optional<Transaction> findTopByOrderByTranIdDesc();
}
