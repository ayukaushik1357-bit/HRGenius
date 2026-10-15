package com.hrgenius.mapper;

import com.hrgenius.dto.payroll.PayrollRunResponse;
import com.hrgenius.dto.payroll.PayslipResponse;
import com.hrgenius.dto.payroll.SalaryStructureResponse;
import com.hrgenius.entity.payroll.PayrollRun;
import com.hrgenius.entity.payroll.Payslip;
import com.hrgenius.entity.payroll.SalaryStructure;
import org.springframework.stereotype.Component;

@Component
public class PayrollMapper {

    public SalaryStructureResponse toSalaryStructureResponse(SalaryStructure entity) {
        if (entity == null) return null;
        String employeeName = null;
        String employeeCode = null;
        Long employeeId = null;
        if (entity.getEmployee() != null) {
            employeeId = entity.getEmployee().getId();
            employeeName = entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName();
            employeeCode = entity.getEmployee().getEmployeeCode();
        }

        return SalaryStructureResponse.builder()
                .id(entity.getId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .basicSalary(entity.getBasicSalary())
                .hra(entity.getHra())
                .specialAllowance(entity.getSpecialAllowance())
                .otherAllowances(entity.getOtherAllowances())
                .monthlyGross(entity.getMonthlyGross())
                .effectiveFrom(entity.getEffectiveFrom())
                .build();
    }

    public PayrollRunResponse toPayrollRunResponse(PayrollRun entity) {
        if (entity == null) return null;
        return PayrollRunResponse.builder()
                .id(entity.getId())
                .year(entity.getYear())
                .month(entity.getMonth())
                .totalEmployees(entity.getTotalEmployees())
                .totalGrossPay(entity.getTotalGrossPay())
                .totalNetPay(entity.getTotalNetPay())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .processedAt(entity.getProcessedAt())
                .paidAt(entity.getPaidAt())
                .build();
    }

    public PayslipResponse toPayslipResponse(Payslip entity) {
        if (entity == null) return null;
        String employeeName = null;
        String employeeCode = null;
        String deptName = null;
        String desigTitle = null;
        Long employeeId = null;

        if (entity.getEmployee() != null) {
            employeeId = entity.getEmployee().getId();
            employeeName = entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName();
            employeeCode = entity.getEmployee().getEmployeeCode();
            if (entity.getEmployee().getDepartment() != null) {
                deptName = entity.getEmployee().getDepartment().getName();
            }
            if (entity.getEmployee().getDesignation() != null) {
                desigTitle = entity.getEmployee().getDesignation().getTitle();
            }
        }

        return PayslipResponse.builder()
                .id(entity.getId())
                .payrollRunId(entity.getPayrollRun() != null ? entity.getPayrollRun().getId() : null)
                .employeeId(employeeId)
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .departmentName(deptName)
                .designationTitle(desigTitle)
                .year(entity.getYear())
                .month(entity.getMonth())
                .totalWorkingDays(entity.getTotalWorkingDays())
                .presentDays(entity.getPresentDays())
                .unpaidLeaveDays(entity.getUnpaidLeaveDays())
                .basicSalary(entity.getBasicSalary())
                .hra(entity.getHra())
                .specialAllowance(entity.getSpecialAllowance())
                .otherAllowances(entity.getOtherAllowances())
                .grossSalary(entity.getGrossSalary())
                .lopDeduction(entity.getLopDeduction())
                .pfDeduction(entity.getPfDeduction())
                .esiDeduction(entity.getEsiDeduction())
                .ptDeduction(entity.getPtDeduction())
                .tdsDeduction(entity.getTdsDeduction())
                .totalDeductions(entity.getTotalDeductions())
                .netSalary(entity.getNetSalary())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .build();
    }
}
