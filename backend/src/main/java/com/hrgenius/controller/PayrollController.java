package com.hrgenius.controller;

import com.hrgenius.dto.payroll.*;
import com.hrgenius.entity.Employee;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.security.UserPrincipal;
import com.hrgenius.service.PayrollService;
import com.lowagie.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
@Tag(name = "Payroll", description = "Salary structures, monthly payroll runs, payslips, and PDF downloads (Indian compliance)")
public class PayrollController {

    private final PayrollService payrollService;
    private final EmployeeRepository employeeRepository;

    // --- Salary Structure ---
    @GetMapping("/salary-structure/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get salary structure for an employee")
    public ResponseEntity<SalaryStructureResponse> getSalaryStructure(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getSalaryStructureByEmployeeId(employeeId));
    }

    @PostMapping("/salary-structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Configure or update employee salary structure")
    public ResponseEntity<SalaryStructureResponse> saveSalaryStructure(@Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.ok(payrollService.saveOrUpdateSalaryStructure(request));
    }

    // --- Payroll Runs ---
    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "List all monthly payroll runs")
    public ResponseEntity<List<PayrollRunResponse>> getAllPayrollRuns() {
        return ResponseEntity.ok(payrollService.getAllPayrollRuns());
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get payroll run details by ID")
    public ResponseEntity<PayrollRunResponse> getPayrollRunById(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPayrollRunById(id));
    }

    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Execute monthly payroll calculation (auto applies attendance & unpaid leave LOP)")
    public ResponseEntity<PayrollRunResponse> processPayroll(@Valid @RequestBody PayrollRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                payrollService.processMonthlyPayroll(request.getYear(), request.getMonth())
        );
    }

    @PostMapping("/runs/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Lock and mark payroll run as PAID (dispatches payslip notifications)")
    public ResponseEntity<PayrollRunResponse> markAsPaid(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.markPayrollAsPaid(id));
    }

    // --- Payslips ---
    @GetMapping("/runs/{id}/payslips")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get all payslips for a payroll run")
    public ResponseEntity<List<PayslipResponse>> getPayslipsByRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPayslipsByRun(id));
    }

    @GetMapping("/my-payslips")
    @Operation(summary = "Get payslips for the authenticated employee")
    public ResponseEntity<List<PayslipResponse>> getMyPayslips(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BadRequestException("User not authenticated");
        }
        Employee emp = employeeRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new BadRequestException("No employee profile found for user"));
        return ResponseEntity.ok(payrollService.getEmployeePayslips(emp.getId()));
    }

    @GetMapping("/employee/{employeeId}/payslips")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get all payslips for a specific employee (ADMIN/HR_MANAGER)")
    public ResponseEntity<List<PayslipResponse>> getEmployeePayslips(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getEmployeePayslips(employeeId));
    }

    @GetMapping("/payslips/{id}")
    @Operation(summary = "Get single payslip details")
    public ResponseEntity<PayslipResponse> getPayslipById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        PayslipResponse payslip = payrollService.getPayslipById(id);
        // Authorization: employees can only view their own
        if (!hasRole(principal, "ROLE_ADMIN") && !hasRole(principal, "ROLE_HR_MANAGER")) {
            Employee emp = employeeRepository.findByUserId(principal.getId()).orElse(null);
            if (emp == null || !emp.getId().equals(payslip.getEmployeeId())) {
                throw new BadRequestException("Access denied: You can only view your own payslips");
            }
        }
        return ResponseEntity.ok(payslip);
    }

    @GetMapping("/payslips/{id}/pdf")
    @Operation(summary = "Download payslip as PDF (OpenPDF)")
    public ResponseEntity<byte[]> downloadPayslipPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) throws DocumentException {
        PayslipResponse payslip = payrollService.getPayslipById(id);
        if (!hasRole(principal, "ROLE_ADMIN") && !hasRole(principal, "ROLE_HR_MANAGER")) {
            Employee emp = employeeRepository.findByUserId(principal.getId()).orElse(null);
            if (emp == null || !emp.getId().equals(payslip.getEmployeeId())) {
                throw new BadRequestException("Access denied: You can only download your own payslips");
            }
        }

        byte[] pdfBytes = payrollService.generatePayslipPdf(id);
        String filename = "payslip_" + payslip.getEmployeeCode() + "_" + payslip.getYear() + "_" + payslip.getMonth() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    private boolean hasRole(UserPrincipal principal, String role) {
        if (principal == null || principal.getAuthorities() == null) return false;
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase(role));
    }
}
