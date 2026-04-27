package com.aws.carddemo.billpay.repository;

import com.aws.carddemo.billpay.model.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t ORDER BY t.tranId DESC LIMIT 1")
    Optional<Transaction> findTopByOrderByTranIdDesc();
}
