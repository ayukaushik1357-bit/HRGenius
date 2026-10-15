package com.hrgenius.repository;

import com.hrgenius.entity.payroll.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByPayrollRunId(Long payrollRunId);
    List<Payslip> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);
    Optional<Payslip> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    @org.springframework.data.jpa.repository.Query("SELECT d.name, SUM(ps.grossSalary) FROM Payslip ps " +
           "JOIN ps.employee e JOIN e.department d " +
           "WHERE ps.year = :year AND ps.month = :month GROUP BY d.name")
    List<Object[]> getPayrollCostByDepartment(@org.springframework.data.repository.query.Param("year") Integer year,
                                              @org.springframework.data.repository.query.Param("month") Integer month);
}

