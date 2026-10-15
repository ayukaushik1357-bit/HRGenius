package com.hrgenius.repository;

import com.hrgenius.entity.payroll.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    Optional<PayrollRun> findByYearAndMonth(Integer year, Integer month);
    boolean existsByYearAndMonth(Integer year, Integer month);
}
