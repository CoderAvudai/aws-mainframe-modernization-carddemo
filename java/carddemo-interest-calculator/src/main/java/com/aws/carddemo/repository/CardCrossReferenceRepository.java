package com.aws.carddemo.repository;

import com.aws.carddemo.entity.CardCrossReference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardCrossReferenceRepository extends JpaRepository<CardCrossReference, String> {

    Optional<CardCrossReference> findFirstByAcctId(Long acctId);
}
