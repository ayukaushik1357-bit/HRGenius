package com.hrgenius.repository;

import com.hrgenius.entity.offboarding.FnFSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FnFSettlementRepository extends JpaRepository<FnFSettlement, Long> {
    Optional<FnFSettlement> findByExitRequestId(Long exitRequestId);
    Optional<FnFSettlement> findByEmployeeId(Long employeeId);
}
