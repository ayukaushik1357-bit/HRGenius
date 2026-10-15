package com.hrgenius.service;

import com.hrgenius.dto.offboarding.ClearanceItemDto;
import com.hrgenius.dto.offboarding.ExitRequestDto;
import com.hrgenius.dto.offboarding.FnFSettlementDto;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.User;
import com.hrgenius.entity.leave.LeaveBalance;
import com.hrgenius.entity.offboarding.ClearanceItem;
import com.hrgenius.entity.offboarding.ExitRequest;
import com.hrgenius.entity.offboarding.FnFSettlement;
import com.hrgenius.entity.payroll.SalaryStructure;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.OffboardingMapper;
import com.hrgenius.repository.*;
import com.hrgenius.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OffboardingService {

    private final ExitRequestRepository exitRequestRepository;
    private final ClearanceItemRepository clearanceItemRepository;
    private final FnFSettlementRepository fnfSettlementRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final OffboardingMapper offboardingMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public ExitRequestDto.Response submitResignation(ExitRequestDto.Request request) {
        final Long targetEmpId = request.getEmployeeId() != null ? request.getEmployeeId() : getCurrentEmployee().getId();

        Employee emp = employeeRepository.findById(targetEmpId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + targetEmpId));

        // Check if there is already an active resignation
        Optional<ExitRequest> existing = exitRequestRepository.findByEmployeeIdAndStatusNot(targetEmpId, ExitRequest.ExitStatus.REJECTED);
        if (existing.isPresent() && existing.get().getStatus() != ExitRequest.ExitStatus.COMPLETED) {
            throw new BadRequestException("An active exit request already exists for this employee.");
        }

        ExitRequest exitRequest = new ExitRequest();
        exitRequest.setEmployee(emp);
        exitRequest.setResignationDate(request.getResignationDate() != null ? request.getResignationDate() : LocalDate.now());
        exitRequest.setNoticePeriodDays(request.getNoticePeriodDays() != null ? request.getNoticePeriodDays() : 30);
        exitRequest.setLastWorkingDay(request.getLastWorkingDay() != null ? request.getLastWorkingDay() : exitRequest.getResignationDate().plusDays(exitRequest.getNoticePeriodDays()));
        exitRequest.setReason(request.getReason());
        exitRequest.setStatus(ExitRequest.ExitStatus.PENDING);

        ExitRequest saved = exitRequestRepository.save(exitRequest);

        // Notify HR and Manager
        if (emp.getUser() != null) {
            notificationService.sendNotification(
                    emp.getUser().getId(),
                    "Resignation Submitted",
                    "Your resignation request has been submitted for review. Target last working day: " + saved.getLastWorkingDay(),
                    "EXIT_REQUEST"
            );
        }

        return offboardingMapper.toExitResponse(saved);
    }

    @Transactional
    public ExitRequestDto.Response processExitRequest(Long id, ExitRequestDto.ActionRequest action) {
        ExitRequest exitReq = exitRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found with id: " + id));

        Employee currentEmp = getCurrentEmployee();

        if (action.isApproved()) {
            exitReq.setStatus(ExitRequest.ExitStatus.APPROVED);
            exitReq.setApprovedBy(currentEmp);
            exitReq.setRemarks(action.getRemarks());
            if (action.getAgreedLastWorkingDay() != null) {
                exitReq.setLastWorkingDay(action.getAgreedLastWorkingDay());
            }

            // Update Employee status to ON_NOTICE
            Employee emp = exitReq.getEmployee();
            String oldStatus = emp.getStatus().name();
            emp.setStatus(Employee.EmployeeStatus.ON_NOTICE);
            employeeRepository.save(emp);

            auditService.logChange("Employee", emp.getId(), "STATUS_CHANGE", oldStatus, "ON_NOTICE");

            // Auto-create standard clearance checklist
            createDefaultClearanceItems(exitReq);

            // Auto-calculate initial Full & Final (FnF)
            calculateAndSaveFnF(exitReq);

            if (emp.getUser() != null) {
                notificationService.sendNotification(
                        emp.getUser().getId(),
                        "Resignation Approved",
                        "Your resignation has been approved. Your last working day is " + exitReq.getLastWorkingDay(),
                        "EXIT_REQUEST"
                );
            }
        } else {
            exitReq.setStatus(ExitRequest.ExitStatus.REJECTED);
            exitReq.setApprovedBy(currentEmp);
            exitReq.setRemarks(action.getRemarks());

            if (exitReq.getEmployee().getUser() != null) {
                notificationService.sendNotification(
                        exitReq.getEmployee().getUser().getId(),
                        "Resignation Not Approved",
                        "Your resignation request was reviewed: " + action.getRemarks(),
                        "EXIT_REQUEST"
                );
            }
        }

        return offboardingMapper.toExitResponse(exitRequestRepository.save(exitReq));
    }

    private void createDefaultClearanceItems(ExitRequest exitReq) {
        if (!exitReq.getClearanceItems().isEmpty()) return;

        createItem(exitReq, ClearanceItem.DepartmentType.IT, "Laptop & Peripheral Assets Return");
        createItem(exitReq, ClearanceItem.DepartmentType.IT, "Corporate Email & VPN Access Revocation");
        createItem(exitReq, ClearanceItem.DepartmentType.FINANCE, "Company Travel & Expense Advances Settlement");
        createItem(exitReq, ClearanceItem.DepartmentType.ADMIN, "ID Badge, Access Card & Office Key Return");
        createItem(exitReq, ClearanceItem.DepartmentType.HR, "Exit Interview & Non-Disclosure Compliance Acknowledgement");
    }

    private void createItem(ExitRequest exitReq, ClearanceItem.DepartmentType dept, String title) {
        ClearanceItem item = new ClearanceItem();
        item.setExitRequest(exitReq);
        item.setDepartmentType(dept);
        item.setTitle(title);
        item.setStatus(ClearanceItem.ClearanceStatus.PENDING);
        clearanceItemRepository.save(item);
    }

    @Transactional
    public ClearanceItemDto.Response updateClearanceItem(Long itemId, ClearanceItemDto.Request request) {
        ClearanceItem item = clearanceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Clearance item not found with id: " + itemId));

        if (request.getStatus() != null) {
            item.setStatus(ClearanceItem.ClearanceStatus.valueOf(request.getStatus().toUpperCase()));
        }
        item.setClearedBy(getCurrentEmployee());
        item.setClearedAt(LocalDateTime.now());
        if (request.getRemarks() != null) {
            item.setRemarks(request.getRemarks());
        }

        return offboardingMapper.toClearanceResponse(clearanceItemRepository.save(item));
    }

    @Transactional
    public FnFSettlementDto.Response calculateAndSaveFnF(ExitRequest exitReq) {
        Employee emp = exitReq.getEmployee();
        LocalDate lastDay = exitReq.getLastWorkingDay();

        // 1. Days payable in last month
        int payableDays = lastDay.getDayOfMonth();

        // 2. Base salary components
        BigDecimal basic = BigDecimal.valueOf(30000);
        BigDecimal hra = BigDecimal.valueOf(15000);
        BigDecimal allowances = BigDecimal.ZERO;

        Optional<SalaryStructure> ssOpt = salaryStructureRepository.findByEmployeeId(emp.getId());
        if (ssOpt.isPresent()) {
            SalaryStructure ss = ssOpt.get();
            basic = ss.getBasicSalary();
            hra = ss.getHra();
            allowances = ss.getSpecialAllowance().add(ss.getOtherAllowances());
        }

        BigDecimal grossMonthly = basic.add(hra).add(allowances);
        BigDecimal perDaySalary = grossMonthly.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        BigDecimal salaryPayable = perDaySalary.multiply(BigDecimal.valueOf(payableDays)).setScale(2, RoundingMode.HALF_UP);

        // 3. Leave Encashment
        double earnedLeaveDays = 0.0;
        Optional<LeaveBalance> lbOpt = leaveBalanceRepository.findByEmployeeIdAndYear(emp.getId(), lastDay.getYear());
        if (lbOpt.isPresent()) {
            earnedLeaveDays = Math.max(0.0, lbOpt.get().getEarnedRemaining());
        }
        BigDecimal dailyBasic = basic.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        BigDecimal leaveEncashmentAmount = dailyBasic.multiply(BigDecimal.valueOf(earnedLeaveDays)).setScale(2, RoundingMode.HALF_UP);

        // 4. Gratuity (Tenure >= 5 years = 60 months)
        BigDecimal gratuityAmount = BigDecimal.ZERO;
        if (emp.getJoiningDate() != null) {
            long months = ChronoUnit.MONTHS.between(emp.getJoiningDate(), lastDay);
            if (months >= 60) {
                double years = (double) months / 12.0;
                // Gratuity formula = 15 * (Basic / 26) * years
                BigDecimal gratuityPerYear = basic.multiply(BigDecimal.valueOf(15)).divide(BigDecimal.valueOf(26), 2, RoundingMode.HALF_UP);
                gratuityAmount = gratuityPerYear.multiply(BigDecimal.valueOf(years)).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal deductions = BigDecimal.ZERO;
        BigDecimal netPayable = salaryPayable.add(leaveEncashmentAmount).add(gratuityAmount).subtract(deductions);

        FnFSettlement fnf = fnfSettlementRepository.findByExitRequestId(exitReq.getId())
                .orElse(new FnFSettlement());

        fnf.setExitRequest(exitReq);
        fnf.setEmployee(emp);
        fnf.setSettlementDate(lastDay);
        fnf.setPayableDays(payableDays);
        fnf.setSalaryPayable(salaryPayable);
        fnf.setLeaveEncashmentDays(earnedLeaveDays);
        fnf.setLeaveEncashmentAmount(leaveEncashmentAmount);
        fnf.setGratuityAmount(gratuityAmount);
        fnf.setDeductionsAmount(deductions);
        fnf.setNetPayableAmount(netPayable);
        fnf.setStatus(FnFSettlement.SettlementStatus.DRAFT);
        fnf.setRemarks("Computed FnF settlement on notice approval.");

        return offboardingMapper.toSettlementResponse(fnfSettlementRepository.save(fnf));
    }

    @Transactional
    public FnFSettlementDto.Response finalizeSettlement(Long settlementId) {
        FnFSettlement settlement = fnfSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("FnF settlement not found with id: " + settlementId));

        ExitRequest exitReq = settlement.getExitRequest();

        // Verify all clearance items are cleared
        boolean allCleared = exitReq.getClearanceItems().stream()
                .allMatch(c -> c.getStatus() == ClearanceItem.ClearanceStatus.CLEARED || c.getStatus() == ClearanceItem.ClearanceStatus.WAIVED);

        if (!allCleared) {
            throw new BadRequestException("Cannot finalize FnF settlement: pending clearance checklist items remain.");
        }

        settlement.setStatus(FnFSettlement.SettlementStatus.PROCESSED);
        fnfSettlementRepository.save(settlement);

        // Mark ExitRequest COMPLETED
        exitReq.setStatus(ExitRequest.ExitStatus.COMPLETED);
        exitRequestRepository.save(exitReq);

        // Update Employee status to EXITED
        Employee emp = settlement.getEmployee();
        String oldStatus = emp.getStatus().name();
        emp.setStatus(Employee.EmployeeStatus.EXITED);
        employeeRepository.save(emp);

        // Disable user login
        if (emp.getUser() != null) {
            User user = emp.getUser();
            user.setEnabled(false);
            userRepository.save(user);
        }

        auditService.logChange("Employee", emp.getId(), "OFFBOARDING_COMPLETED", oldStatus, "EXITED");

        return offboardingMapper.toSettlementResponse(settlement);
    }

    @Transactional(readOnly = true)
    public List<ExitRequestDto.Response> getAllExitRequests() {
        return exitRequestRepository.findAll().stream()
                .map(offboardingMapper::toExitResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExitRequestDto.Response getExitRequestById(Long id) {
        ExitRequest exitRequest = exitRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found with id: " + id));
        return offboardingMapper.toExitResponse(exitRequest);
    }

    @Transactional(readOnly = true)
    public List<ExitRequestDto.Response> getMyExitRequests() {
        Employee emp = getCurrentEmployee();
        return exitRequestRepository.findByEmployeeId(emp.getId()).stream()
                .map(offboardingMapper::toExitResponse)
                .toList();
    }

    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)) {
            return employeeRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("No employee found in session."));
        }
        return employeeRepository.findByUserId(up.getId())
                .orElseGet(() -> employeeRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new BadRequestException("Employee record not linked to user.")));
    }
}
