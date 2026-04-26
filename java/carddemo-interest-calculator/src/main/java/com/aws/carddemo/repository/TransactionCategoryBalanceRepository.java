package com.aws.carddemo.repository;

import com.aws.carddemo.entity.TransactionCategoryBalance;
import com.aws.carddemo.entity.TransactionCategoryBalanceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionCategoryBalanceRepository
        extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {

    List<TransactionCategoryBalance> findAllByOrderByAcctIdAscTypeCdAscCatCdAsc();

    List<TransactionCategoryBalance> findByAcctIdOrderByTypeCdAscCatCdAsc(Long acctId);
}
