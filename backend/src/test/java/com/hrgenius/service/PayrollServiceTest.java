package com.hrgenius.service;

import com.hrgenius.dto.payroll.PayrollRunResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.payroll.PayrollRun;
import com.hrgenius.entity.payroll.Payslip;
import com.hrgenius.entity.payroll.SalaryStructure;
import com.hrgenius.mapper.PayrollMapper;
import com.hrgenius.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private SalaryStructureRepository salaryStructureRepository;

    @Mock
    private PayrollRunRepository payrollRunRepository;

    @Mock
    private PayslipRepository payslipRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private PayrollMapper payrollMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PayrollService payrollService;

    private SalaryStructure salaryStructure;
    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("EMP001");
        employee.setFirstName("Bob");
        employee.setLastName("Jones");
        employee.setStatus(Employee.EmployeeStatus.ACTIVE);
        employee.setJoiningDate(LocalDate.of(2025, 1, 1));

        salaryStructure = new SalaryStructure();
        salaryStructure.setEmployee(employee);
        salaryStructure.setBasicSalary(BigDecimal.valueOf(50000.00));
        salaryStructure.setHra(BigDecimal.valueOf(25000.00));
        salaryStructure.setSpecialAllowance(BigDecimal.valueOf(15000.00));
        salaryStructure.setOtherAllowances(BigDecimal.valueOf(5000.00));
    }

    @Test
    @DisplayName("Should correctly calculate payslip for standard month without LOP")
    void calculatePayslip_standardMonth_noLOP() {
        Payslip payslip = new Payslip();

        // 30 days in month (April), 0 unpaid leaves, 22 present days
        payrollService.calculatePayslip(payslip, salaryStructure, 30, 0.0, 22.0);

        // Gross = 50,000 + 25,000 + 15,000 + 5,000 = 95,000
        assertThat(payslip.getGrossSalary()).isEqualByComparingTo(BigDecimal.valueOf(95000.00));
        assertThat(payslip.getLopDeduction()).isEqualByComparingTo(BigDecimal.ZERO);

        // PF = 12% of 50,000 = 6,000
        assertThat(payslip.getPfDeduction()).isEqualByComparingTo(BigDecimal.valueOf(6000.00));

        // ESI is 0 since Gross (95,000) > 21,000 threshold
        assertThat(payslip.getEsiDeduction()).isEqualByComparingTo(BigDecimal.ZERO);

        // PT = 200
        assertThat(payslip.getPtDeduction()).isEqualByComparingTo(BigDecimal.valueOf(200.00));

        // Net should be gross minus deductions
        assertThat(payslip.getNetSalary()).isEqualByComparingTo(
                payslip.getGrossSalary().subtract(payslip.getTotalDeductions()));
    }

    @Test
    @DisplayName("Should deduct Loss of Pay (LOP) and adjust PF for unpaid leaves")
    void calculatePayslip_withLossOfPay_deductsCorrectly() {
        Payslip payslip = new Payslip();

        // 30 days in month, 3 unpaid leave days
        payrollService.calculatePayslip(payslip, salaryStructure, 30, 3.0, 19.0);

        // Daily gross = 95,000 / 30 = 3166.6667 -> 3 days = 9500.00
        assertThat(payslip.getLopDeduction()).isEqualByComparingTo(BigDecimal.valueOf(9500.00));

        // Effective basic = 50,000 - (50,000/30 * 3) = 45,000
        // PF = 12% of 45,000 = 5,400.00
        assertThat(payslip.getPfDeduction()).isEqualByComparingTo(BigDecimal.valueOf(5400.00));
    }

    @Test
    @DisplayName("Should apply 0.75% ESI deduction when gross salary is <= 21,000")
    void calculatePayslip_lowIncome_appliesESI() {
        SalaryStructure lowIncomeStructure = new SalaryStructure();
        lowIncomeStructure.setBasicSalary(BigDecimal.valueOf(10000.00));
        lowIncomeStructure.setHra(BigDecimal.valueOf(5000.00));
        lowIncomeStructure.setSpecialAllowance(BigDecimal.valueOf(3000.00));
        lowIncomeStructure.setOtherAllowances(BigDecimal.valueOf(0.00));

        Payslip payslip = new Payslip();
        // Gross = 18,000 (<= 21,000)
        payrollService.calculatePayslip(payslip, lowIncomeStructure, 30, 0.0, 22.0);

        assertThat(payslip.getGrossSalary()).isEqualByComparingTo(BigDecimal.valueOf(18000.00));
        // ESI = 0.75% of 18,000 = 135.00
        assertThat(payslip.getEsiDeduction()).isEqualByComparingTo(BigDecimal.valueOf(135.00));
    }

    @Test
    @DisplayName("Should correctly prorate salary for mid-month joining employee")
    void processMonthlyPayroll_midMonthJoiner_proratesCorrectly() {
        // Employee joins on April 16, 2026 (Month has 30 days)
        employee.setJoiningDate(LocalDate.of(2026, 4, 16));

        when(payrollRunRepository.findByYearAndMonth(2026, 4)).thenReturn(Optional.empty());
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(salaryStructureRepository.findByEmployeeId(1L)).thenReturn(Optional.of(salaryStructure));
        when(leaveApplicationRepository.sumUnpaidLeaveDays(eq(1L), any(), any())).thenReturn(0.0);
        when(attendanceRepository.countPresentDays(eq(1L), any(), any())).thenReturn(10L);
        when(attendanceRepository.countHalfDays(eq(1L), any(), any())).thenReturn(0L);
        when(payslipRepository.findByEmployeeIdAndYearAndMonth(1L, 2026, 4)).thenReturn(Optional.empty());
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> i.getArgument(0));

        PayrollRunResponse expectedResp = PayrollRunResponse.builder()
                .year(2026)
                .month(4)
                .totalEmployees(1)
                .status("PROCESSED")
                .build();
        when(payrollMapper.toPayrollRunResponse(any(PayrollRun.class))).thenReturn(expectedResp);

        PayrollRunResponse response = payrollService.processMonthlyPayroll(2026, 4);

        assertThat(response).isNotNull();
        assertThat(response.getTotalEmployees()).isEqualTo(1);
        verify(payrollRunRepository).save(any(PayrollRun.class));
    }
}
