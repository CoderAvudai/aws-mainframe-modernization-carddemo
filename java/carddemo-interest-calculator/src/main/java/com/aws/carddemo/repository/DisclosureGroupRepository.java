package com.aws.carddemo.repository;

import com.aws.carddemo.entity.DisclosureGroup;
import com.aws.carddemo.entity.DisclosureGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, DisclosureGroupId> {
}
