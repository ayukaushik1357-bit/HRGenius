package com.hrgenius.service;

import com.hrgenius.dto.payroll.*;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.notification.AppNotification;
import com.hrgenius.entity.payroll.PayrollRun;
import com.hrgenius.entity.payroll.Payslip;
import com.hrgenius.entity.payroll.SalaryStructure;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.PayrollMapper;
import com.hrgenius.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollMapper payrollMapper;
    private final NotificationService notificationService;

    // --- Salary Structure ---
    @Transactional(readOnly = true)
    public SalaryStructureResponse getSalaryStructureByEmployeeId(Long employeeId) {
        SalaryStructure ss = salaryStructureRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure for employee", "id", employeeId));
        return payrollMapper.toSalaryStructureResponse(ss);
    }

    @Transactional
    public SalaryStructureResponse saveOrUpdateSalaryStructure(SalaryStructureRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        SalaryStructure ss = salaryStructureRepository.findByEmployeeId(employee.getId())
                .orElseGet(() -> {
                    SalaryStructure s = new SalaryStructure();
                    s.setEmployee(employee);
                    return s;
                });

        ss.setBasicSalary(request.getBasicSalary());
        ss.setHra(request.getHra());
        ss.setSpecialAllowance(request.getSpecialAllowance() != null ? request.getSpecialAllowance() : BigDecimal.ZERO);
        ss.setOtherAllowances(request.getOtherAllowances() != null ? request.getOtherAllowances() : BigDecimal.ZERO);
        ss.setEffectiveFrom(request.getEffectiveFrom());

        SalaryStructure saved = salaryStructureRepository.save(ss);
        return payrollMapper.toSalaryStructureResponse(saved);
    }

    // --- Payroll Run ---
    @Transactional(readOnly = true)
    public List<PayrollRunResponse> getAllPayrollRuns() {
        return payrollRunRepository.findAll().stream()
                .map(payrollMapper::toPayrollRunResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse getPayrollRunById(Long id) {
        PayrollRun run = payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", id));
        return payrollMapper.toPayrollRunResponse(run);
    }

    @Transactional
    public PayrollRunResponse processMonthlyPayroll(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        int totalDaysInMonth = ym.lengthOfMonth();

        PayrollRun run = payrollRunRepository.findByYearAndMonth(year, month).orElseGet(() -> {
            PayrollRun pr = new PayrollRun();
            pr.setYear(year);
            pr.setMonth(month);
            pr.setStatus(PayrollRun.PayrollStatus.DRAFT);
            return pr;
        });

        if (run.getStatus() == PayrollRun.PayrollStatus.PAID) {
            throw new BadRequestException("Payroll for " + ym + " is already PAID and locked against modification");
        }

        // Fetch all active employees
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE)
                .collect(Collectors.toList());

        List<Payslip> payslips = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (Employee emp : activeEmployees) {
            // Check joining date: if joined after monthEnd, skip
            if (emp.getJoiningDate().isAfter(monthEnd)) {
                continue;
            }

            SalaryStructure ss = salaryStructureRepository.findByEmployeeId(emp.getId()).orElse(null);
            if (ss == null) {
                // If no salary structure is defined, skip or create with 0
                continue;
            }

            // Calculate Unpaid Leave Days (Loss of Pay)
            Double unpaidLeaves = leaveApplicationRepository.sumUnpaidLeaveDays(emp.getId(), monthStart, monthEnd);
            double unpaidLeaveDays = unpaidLeaves != null ? unpaidLeaves : 0.0;

            // Handle mid-month joining: days before joiningDate in this month are unpaid
            if (emp.getJoiningDate().isAfter(monthStart)) {
                long daysBeforeJoining = emp.getJoiningDate().getDayOfMonth() - 1;
                unpaidLeaveDays += daysBeforeJoining;
            }

            // Cap unpaid leave days to total days in month
            unpaidLeaveDays = Math.min(totalDaysInMonth, unpaidLeaveDays);

            // Present days
            long presentCount = attendanceRepository.countPresentDays(emp.getId(), monthStart, monthEnd);
            long halfCount = attendanceRepository.countHalfDays(emp.getId(), monthStart, monthEnd);
            double presentDays = presentCount + (halfCount * 0.5);

            Payslip payslip = payslipRepository.findByEmployeeIdAndYearAndMonth(emp.getId(), year, month)
                    .orElseGet(() -> {
                        Payslip p = new Payslip();
                        p.setPayrollRun(run);
                        p.setEmployee(emp);
                        p.setYear(year);
                        p.setMonth(month);
                        return p;
                    });

            calculatePayslip(payslip, ss, totalDaysInMonth, unpaidLeaveDays, presentDays);
            payslips.add(payslip);

            totalGross = totalGross.add(payslip.getGrossSalary());
            totalNet = totalNet.add(payslip.getNetSalary());
        }

        run.setTotalEmployees(payslips.size());
        run.setTotalGrossPay(totalGross);
        run.setTotalNetPay(totalNet);
        run.setStatus(PayrollRun.PayrollStatus.PROCESSED);
        run.setProcessedAt(LocalDateTime.now());
        run.getPayslips().clear();
        run.getPayslips().addAll(payslips);

        PayrollRun saved = payrollRunRepository.save(run);
        return payrollMapper.toPayrollRunResponse(saved);
    }

    public void calculatePayslip(Payslip payslip, SalaryStructure ss, int totalDaysInMonth, double unpaidLeaveDays, double presentDays) {
        BigDecimal basic = ss.getBasicSalary();
        BigDecimal hra = ss.getHra();
        BigDecimal special = ss.getSpecialAllowance() != null ? ss.getSpecialAllowance() : BigDecimal.ZERO;
        BigDecimal other = ss.getOtherAllowances() != null ? ss.getOtherAllowances() : BigDecimal.ZERO;
        BigDecimal gross = basic.add(hra).add(special).add(other);

        // LOP deduction
        BigDecimal dailyRate = gross.divide(BigDecimal.valueOf(totalDaysInMonth), 4, RoundingMode.HALF_UP);
        BigDecimal lopDeduction = dailyRate.multiply(BigDecimal.valueOf(unpaidLeaveDays)).setScale(2, RoundingMode.HALF_UP);

        // Effective basic after LOP (for PF)
        BigDecimal dailyBasic = basic.divide(BigDecimal.valueOf(totalDaysInMonth), 4, RoundingMode.HALF_UP);
        BigDecimal basicLop = dailyBasic.multiply(BigDecimal.valueOf(unpaidLeaveDays)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal effectiveBasic = basic.subtract(basicLop).max(BigDecimal.ZERO);

        // PF: 12% of basic (or effective basic)
        BigDecimal pfDeduction = effectiveBasic.multiply(BigDecimal.valueOf(0.12)).setScale(2, RoundingMode.HALF_UP);

        // ESI: 0.75% of gross if monthly gross <= 21000
        BigDecimal esiDeduction = BigDecimal.ZERO;
        BigDecimal effectiveGross = gross.subtract(lopDeduction).max(BigDecimal.ZERO);
        if (gross.compareTo(BigDecimal.valueOf(21000)) <= 0) {
            esiDeduction = effectiveGross.multiply(BigDecimal.valueOf(0.0075)).setScale(2, RoundingMode.HALF_UP);
        }

        // Professional Tax: standard INR 200/month if gross >= 10000
        BigDecimal ptDeduction = effectiveGross.compareTo(BigDecimal.valueOf(10000)) >= 0 ?
                BigDecimal.valueOf(200.00) : BigDecimal.ZERO;

        // TDS: simplified annual slab calculation
        BigDecimal tdsDeduction = calculateMonthlyTDS(gross);

        BigDecimal totalDeductions = lopDeduction.add(pfDeduction).add(esiDeduction).add(ptDeduction).add(tdsDeduction);
        BigDecimal netSalary = gross.subtract(totalDeductions).max(BigDecimal.ZERO);

        payslip.setTotalWorkingDays(totalDaysInMonth);
        payslip.setPresentDays(presentDays);
        payslip.setUnpaidLeaveDays(unpaidLeaveDays);
        payslip.setBasicSalary(basic);
        payslip.setHra(hra);
        payslip.setSpecialAllowance(special);
        payslip.setOtherAllowances(other);
        payslip.setGrossSalary(gross);
        payslip.setLopDeduction(lopDeduction);
        payslip.setPfDeduction(pfDeduction);
        payslip.setEsiDeduction(esiDeduction);
        payslip.setPtDeduction(ptDeduction);
        payslip.setTdsDeduction(tdsDeduction);
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetSalary(netSalary);
        payslip.setStatus(PayrollRun.PayrollStatus.PROCESSED);
    }

    public BigDecimal calculateMonthlyTDS(BigDecimal monthlyGross) {
        BigDecimal annualGross = monthlyGross.multiply(BigDecimal.valueOf(12));
        // Standard deduction 75,000
        BigDecimal taxable = annualGross.subtract(BigDecimal.valueOf(75000)).max(BigDecimal.ZERO);

        BigDecimal annualTax = BigDecimal.ZERO;
        if (taxable.compareTo(BigDecimal.valueOf(300000)) <= 0) {
            annualTax = BigDecimal.ZERO;
        } else if (taxable.compareTo(BigDecimal.valueOf(700000)) <= 0) {
            annualTax = taxable.subtract(BigDecimal.valueOf(300000)).multiply(BigDecimal.valueOf(0.05));
        } else if (taxable.compareTo(BigDecimal.valueOf(1000000)) <= 0) {
            annualTax = BigDecimal.valueOf(20000).add(taxable.subtract(BigDecimal.valueOf(700000)).multiply(BigDecimal.valueOf(0.10)));
        } else if (taxable.compareTo(BigDecimal.valueOf(1200000)) <= 0) {
            annualTax = BigDecimal.valueOf(50000).add(taxable.subtract(BigDecimal.valueOf(1000000)).multiply(BigDecimal.valueOf(0.15)));
        } else if (taxable.compareTo(BigDecimal.valueOf(1500000)) <= 0) {
            annualTax = BigDecimal.valueOf(80000).add(taxable.subtract(BigDecimal.valueOf(1200000)).multiply(BigDecimal.valueOf(0.20)));
        } else {
            annualTax = BigDecimal.valueOf(140000).add(taxable.subtract(BigDecimal.valueOf(1500000)).multiply(BigDecimal.valueOf(0.30)));
        }

        return annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public PayrollRunResponse markPayrollAsPaid(Long payrollRunId) {
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", payrollRunId));

        if (run.getStatus() == PayrollRun.PayrollStatus.PAID) {
            throw new BadRequestException("Payroll is already marked as PAID");
        }

        run.setStatus(PayrollRun.PayrollStatus.PAID);
        run.setPaidAt(LocalDateTime.now());
        for (Payslip p : run.getPayslips()) {
            p.setStatus(PayrollRun.PayrollStatus.PAID);
            if (p.getEmployee().getUser() != null) {
                notificationService.notifyUser(
                        p.getEmployee().getUser(),
                        "Salary Credited - " + run.getMonth() + "/" + run.getYear(),
                        "Your salary for " + run.getMonth() + "/" + run.getYear() + " has been processed and paid. Net Pay: INR " + p.getNetSalary(),
                        AppNotification.NotificationType.PAYROLL,
                        "/payroll/payslips"
                );
            }
        }

        PayrollRun saved = payrollRunRepository.save(run);
        return payrollMapper.toPayrollRunResponse(saved);
    }

    // --- Payslips ---
    @Transactional(readOnly = true)
    public List<PayslipResponse> getPayslipsByRun(Long payrollRunId) {
        return payslipRepository.findByPayrollRunId(payrollRunId).stream()
                .map(payrollMapper::toPayslipResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayslipResponse> getEmployeePayslips(Long employeeId) {
        return payslipRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId).stream()
                .map(payrollMapper::toPayslipResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayslipResponse getPayslipById(Long payslipId) {
        Payslip p = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", "id", payslipId));
        return payrollMapper.toPayslipResponse(p);
    }

    // --- PDF Generation with OpenPDF ---
    // Read-only transaction: the PDF reads the employee, department and designation (lazy associations)
    @Transactional(readOnly = true)
    public byte[] generatePayslipPdf(Long payslipId) throws DocumentException {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", "id", payslipId));

        Employee emp = payslip.getEmployee();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);

        document.open();

        // Title Header
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 58, 138));
        Paragraph title = new Paragraph("HRGenius Enterprise Solutions", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY);
        Paragraph subtitle = new Paragraph("MONTHLY SALARY SLIP — " + payslip.getMonth() + "/" + payslip.getYear(), subFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(15);
        document.add(subtitle);

        // Employee Info Table
        PdfPTable empTable = new PdfPTable(4);
        empTable.setWidthPercentage(100);
        empTable.setSpacingAfter(15);

        addCell(empTable, "Employee Name:", true);
        addCell(empTable, emp.getFirstName() + " " + emp.getLastName(), false);
        addCell(empTable, "Employee Code:", true);
        addCell(empTable, emp.getEmployeeCode(), false);

        addCell(empTable, "Department:", true);
        addCell(empTable, emp.getDepartment() != null ? emp.getDepartment().getName() : "N/A", false);
        addCell(empTable, "Designation:", true);
        addCell(empTable, emp.getDesignation() != null ? emp.getDesignation().getTitle() : "N/A", false);

        addCell(empTable, "Total Working Days:", true);
        addCell(empTable, String.valueOf(payslip.getTotalWorkingDays()), false);
        addCell(empTable, "Unpaid Leave Days (LOP):", true);
        addCell(empTable, String.valueOf(payslip.getUnpaidLeaveDays()), false);

        document.add(empTable);

        // Earnings vs Deductions Table
        PdfPTable salaryTable = new PdfPTable(4);
        salaryTable.setWidthPercentage(100);
        salaryTable.setWidths(new float[]{30, 20, 30, 20});
        salaryTable.setSpacingAfter(15);

        // Header Row
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Color headerColor = new Color(30, 58, 138);

        PdfPCell c1 = new PdfPCell(new Phrase("Earnings", headerFont));
        c1.setBackgroundColor(headerColor);
        c1.setPadding(6);
        salaryTable.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase("Amount (INR)", headerFont));
        c2.setBackgroundColor(headerColor);
        c2.setPadding(6);
        salaryTable.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase("Deductions", headerFont));
        c3.setBackgroundColor(headerColor);
        c3.setPadding(6);
        salaryTable.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase("Amount (INR)", headerFont));
        c4.setBackgroundColor(headerColor);
        c4.setPadding(6);
        salaryTable.addCell(c4);

        // Data rows
        addCell(salaryTable, "Basic Salary", false);
        addCell(salaryTable, payslip.getBasicSalary().toString(), false);
        addCell(salaryTable, "Loss of Pay (LOP)", false);
        addCell(salaryTable, payslip.getLopDeduction().toString(), false);

        addCell(salaryTable, "House Rent Allowance (HRA)", false);
        addCell(salaryTable, payslip.getHra().toString(), false);
        addCell(salaryTable, "Provident Fund (PF 12%)", false);
        addCell(salaryTable, payslip.getPfDeduction().toString(), false);

        addCell(salaryTable, "Special Allowance", false);
        addCell(salaryTable, payslip.getSpecialAllowance().toString(), false);
        addCell(salaryTable, "ESI (0.75%)", false);
        addCell(salaryTable, payslip.getEsiDeduction().toString(), false);

        addCell(salaryTable, "Other Allowances", false);
        addCell(salaryTable, payslip.getOtherAllowances().toString(), false);
        addCell(salaryTable, "Professional Tax (PT)", false);
        addCell(salaryTable, payslip.getPtDeduction().toString(), false);

        addCell(salaryTable, "", false);
        addCell(salaryTable, "", false);
        addCell(salaryTable, "TDS / Income Tax", false);
        addCell(salaryTable, payslip.getTdsDeduction().toString(), false);

        // Total Gross & Deductions
        addCell(salaryTable, "Gross Earnings", true);
        addCell(salaryTable, "INR " + payslip.getGrossSalary().toString(), true);
        addCell(salaryTable, "Total Deductions", true);
        addCell(salaryTable, "INR " + payslip.getTotalDeductions().toString(), true);

        document.add(salaryTable);

        // Net Pay Box
        Font netFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(22, 101, 52));
        Paragraph netPayPara = new Paragraph("NET PAYABLE AMOUNT: INR " + payslip.getNetSalary() + "  (" + payslip.getStatus() + ")", netFont);
        netPayPara.setAlignment(Element.ALIGN_CENTER);
        netPayPara.setSpacingAfter(25);
        document.add(netPayPara);

        // Footer note
        Font footFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
        Paragraph footer = new Paragraph("This is a computer generated salary document and does not require a physical signature.", footFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, boolean bold) {
        Font font = bold ?
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9) :
                FontFactory.getFont(FontFactory.HELVETICA, 9);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
