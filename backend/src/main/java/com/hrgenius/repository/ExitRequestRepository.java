package com.hrgenius.repository;

import com.hrgenius.entity.offboarding.ExitRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExitRequestRepository extends JpaRepository<ExitRequest, Long> {
    Optional<ExitRequest> findByEmployeeIdAndStatusNot(Long employeeId, ExitRequest.ExitStatus status);
    List<ExitRequest> findByEmployeeId(Long employeeId);
    List<ExitRequest> findByStatus(ExitRequest.ExitStatus status);
}
