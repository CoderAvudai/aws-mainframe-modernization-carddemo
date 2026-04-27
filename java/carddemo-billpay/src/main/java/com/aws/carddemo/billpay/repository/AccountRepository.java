package com.aws.carddemo.billpay.repository;

import com.aws.carddemo.billpay.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
