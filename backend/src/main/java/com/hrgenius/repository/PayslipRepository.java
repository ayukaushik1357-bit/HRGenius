package com.hrgenius.repository;

import com.hrgenius.entity.payroll.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByPayrollRunId(Long payrollRunId);
    List<Payslip> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);
    Optional<Payslip> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);
}
