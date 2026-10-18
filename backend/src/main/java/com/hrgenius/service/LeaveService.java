package com.hrgenius.service;

import com.hrgenius.dto.leave.LeaveApplicationRequest;
import com.hrgenius.dto.leave.LeaveApplicationResponse;
import com.hrgenius.dto.leave.LeaveApprovalRequest;
import com.hrgenius.dto.leave.LeaveBalanceResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.leave.LeaveApplication;
import com.hrgenius.entity.leave.LeaveBalance;
import com.hrgenius.entity.notification.AppNotification;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.LeaveMapper;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.LeaveApplicationRepository;
import com.hrgenius.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveMapper leaveMapper;
    private final NotificationService notificationService;
    private final com.hrgenius.security.CurrentUserService currentUserService;

    @Transactional
    public LeaveBalance getOrCreateBalance(Long employeeId, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).orElseGet(() -> {
            Employee emp = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
            LeaveBalance balance = new LeaveBalance();
            balance.setEmployee(emp);
            balance.setYear(year);
            balance.setCasualQuota(12.0);
            balance.setCasualUsed(0.0);
            balance.setSickQuota(10.0);
            balance.setSickUsed(0.0);
            balance.setEarnedQuota(15.0);
            balance.setEarnedUsed(0.0);
            return leaveBalanceRepository.save(balance);
        });
    }

    @Transactional(readOnly = true)
    public LeaveBalanceResponse getLeaveBalance(Long employeeId, Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, targetYear)
                .orElseGet(() -> {
                    Employee emp = employeeRepository.findById(employeeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
                    LeaveBalance b = new LeaveBalance();
                    b.setEmployee(emp);
                    b.setYear(targetYear);
                    return b;
                });
        return leaveMapper.toBalanceResponse(balance);
    }

    @Transactional
    public LeaveApplicationResponse applyLeave(Long employeeId, LeaveApplicationRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        // Check for overlapping active leaves
        long overlapping = leaveApplicationRepository.countOverlappingLeaves(
                employeeId, request.getStartDate(), request.getEndDate(), null);
        if (overlapping > 0) {
            throw new BadRequestException("You already have an existing leave overlapping with this date range");
        }

        LeaveApplication.LeaveType type = LeaveApplication.LeaveType.valueOf(request.getLeaveType().toUpperCase());
        int leaveYear = request.getStartDate().getYear();
        LeaveBalance balance = getOrCreateBalance(employeeId, leaveYear);

        // Balance validation (except UNPAID)
        if (type == LeaveApplication.LeaveType.CASUAL) {
            if (balance.getCasualRemaining() < request.getNumberOfDays()) {
                throw new BadRequestException("Insufficient Casual leave balance. Available: " +
                        balance.getCasualRemaining() + ", Requested: " + request.getNumberOfDays());
            }
        } else if (type == LeaveApplication.LeaveType.SICK) {
            if (balance.getSickRemaining() < request.getNumberOfDays()) {
                throw new BadRequestException("Insufficient Sick leave balance. Available: " +
                        balance.getSickRemaining() + ", Requested: " + request.getNumberOfDays());
            }
        } else if (type == LeaveApplication.LeaveType.EARNED) {
            if (balance.getEarnedRemaining() < request.getNumberOfDays()) {
                throw new BadRequestException("Insufficient Earned leave balance. Available: " +
                        balance.getEarnedRemaining() + ", Requested: " + request.getNumberOfDays());
            }
        }

        LeaveApplication app = new LeaveApplication();
        app.setEmployee(employee);
        app.setLeaveType(type);
        app.setStartDate(request.getStartDate());
        app.setEndDate(request.getEndDate());
        app.setNumberOfDays(request.getNumberOfDays());
        app.setReason(request.getReason());
        app.setStatus(LeaveApplication.LeaveStatus.PENDING);

        LeaveApplication saved = leaveApplicationRepository.save(app);

        // Notify reporting manager
        if (employee.getReportingManager() != null && employee.getReportingManager().getUser() != null) {
            notificationService.notifyUser(
                    employee.getReportingManager().getUser(),
                    "New Leave Application",
                    employee.getFirstName() + " " + employee.getLastName() + " requested " +
                            request.getNumberOfDays() + " days of " + type + " leave from " +
                            request.getStartDate() + " to " + request.getEndDate(),
                    AppNotification.NotificationType.LEAVE,
                    "/leaves/approvals"
            );
        }

        return leaveMapper.toApplicationResponse(saved);
    }

    @Transactional
    public LeaveApplicationResponse processLeaveApproval(Long leaveId, Long approverEmployeeId, LeaveApprovalRequest approvalRequest) {
        LeaveApplication app = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveApplication", "id", leaveId));

        if (app.getStatus() != LeaveApplication.LeaveStatus.PENDING) {
            throw new BadRequestException("Leave application is already " + app.getStatus());
        }

        // Nobody may approve their own leave
        if (approverEmployeeId != null && approverEmployeeId.equals(app.getEmployee().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot approve or reject your own leave application");
        }

        // Managers can only approve direct reports; HR/ADMIN can approve any
        boolean isHrOrAdmin = currentUserService != null && currentUserService.isHrOrAdmin();
        if (!isHrOrAdmin) {
            Employee applicant = app.getEmployee();
            if (applicant.getReportingManager() == null || approverEmployeeId == null ||
                    !approverEmployeeId.equals(applicant.getReportingManager().getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Managers can only approve or reject leaves for their direct reports");
            }
        }

        Employee approver = approverEmployeeId != null ? employeeRepository.findById(approverEmployeeId).orElse(null) : null;
        app.setApprovedBy(approver);
        app.setActionedAt(LocalDateTime.now());

        if (approvalRequest.isApproved()) {
            app.setStatus(LeaveApplication.LeaveStatus.APPROVED);

            // Deduct quota
            int leaveYear = app.getStartDate().getYear();
            LeaveBalance balance = getOrCreateBalance(app.getEmployee().getId(), leaveYear);

            switch (app.getLeaveType()) {
                case CASUAL -> balance.setCasualUsed(balance.getCasualUsed() + app.getNumberOfDays());
                case SICK -> balance.setSickUsed(balance.getSickUsed() + app.getNumberOfDays());
                case EARNED -> balance.setEarnedUsed(balance.getEarnedUsed() + app.getNumberOfDays());
                case UNPAID -> {
                    // Unpaid leaves do not reduce quota balances, but affect payroll as LOP
                }
            }
            leaveBalanceRepository.save(balance);

            if (app.getEmployee().getUser() != null) {
                notificationService.notifyUser(
                        app.getEmployee().getUser(),
                        "Leave Approved",
                        "Your " + app.getLeaveType() + " leave from " + app.getStartDate() + " to " + app.getEndDate() + " has been approved.",
                        AppNotification.NotificationType.LEAVE,
                        "/leaves/my"
                );
            }
        } else {
            app.setStatus(LeaveApplication.LeaveStatus.REJECTED);
            app.setRejectionReason(approvalRequest.getRejectionReason());

            if (app.getEmployee().getUser() != null) {
                notificationService.notifyUser(
                        app.getEmployee().getUser(),
                        "Leave Rejected",
                        "Your leave request from " + app.getStartDate() + " to " + app.getEndDate() +
                                " was rejected. Reason: " + approvalRequest.getRejectionReason(),
                        AppNotification.NotificationType.LEAVE,
                        "/leaves/my"
                );
            }
        }

        LeaveApplication saved = leaveApplicationRepository.save(app);
        return leaveMapper.toApplicationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getEmployeeLeaves(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(leaveMapper::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getPendingApprovals(Long managerId) {
        return leaveApplicationRepository.findPendingByManagerId(managerId).stream()
                .map(leaveMapper::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getAllLeaves(LeaveApplication.LeaveStatus status) {
        List<LeaveApplication> list = status == null ?
                leaveApplicationRepository.findAll() :
                leaveApplicationRepository.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(leaveMapper::toApplicationResponse).collect(Collectors.toList());
    }
}
