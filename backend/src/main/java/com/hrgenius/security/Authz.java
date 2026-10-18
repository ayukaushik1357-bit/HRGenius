package com.hrgenius.security;

import com.hrgenius.entity.User;
import com.hrgenius.entity.leave.LeaveApplication;
import com.hrgenius.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("authz")
@RequiredArgsConstructor
public class Authz {

    private final CurrentUserService currentUserService;
    private final EmployeeRepository employeeRepository;
    private final DocumentRepository documentRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final PayslipRepository payslipRepository;
    private final NotificationRepository notificationRepository;
    private final PerformanceReviewRepository performanceReviewRepository;

    public boolean isSelf(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        return currentUserService.getCurrentEmployeeId()
                .map(id -> id.equals(employeeId))
                .orElse(false);
    }

    public boolean isNotSelf(Long employeeId) {
        return !isSelf(employeeId);
    }

    public boolean isManagerOf(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        Long currentEmpId = currentUserService.getCurrentEmployeeId().orElse(null);
        if (currentEmpId == null) {
            return false;
        }
        return employeeRepository.findById(employeeId)
                .map(emp -> emp.getReportingManager() != null && currentEmpId.equals(emp.getReportingManager().getId()))
                .orElse(false);
    }

    public boolean canViewEmployee(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        if (currentUserService.isHrOrAdmin()) {
            return true;
        }
        return isSelf(employeeId) || isManagerOf(employeeId);
    }

    public boolean canActOnEmployee(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        if (currentUserService.isHrOrAdmin()) {
            return true;
        }
        if (currentUserService.isManager()) {
            return isSelf(employeeId) || isManagerOf(employeeId);
        }
        return isSelf(employeeId);
    }

    public boolean canAccessDocument(Long documentId) {
        if (documentId == null) {
            return false;
        }
        if (currentUserService.isHrOrAdmin()) {
            return true;
        }
        Long currentEmpId = currentUserService.getCurrentEmployeeId().orElse(null);
        if (currentEmpId == null) {
            return false;
        }
        return documentRepository.findById(documentId)
                .map(doc -> doc.getEmployee() != null && currentEmpId.equals(doc.getEmployee().getId()))
                .orElse(false);
    }

    public boolean canApproveLeave(Long leaveApplicationId) {
        if (leaveApplicationId == null) {
            return false;
        }
        LeaveApplication leave = leaveApplicationRepository.findById(leaveApplicationId).orElse(null);
        if (leave == null || leave.getEmployee() == null) {
            return false;
        }
        Long applicantId = leave.getEmployee().getId();
        // Nobody may approve their own leave
        if (isSelf(applicantId)) {
            return false;
        }
        if (currentUserService.isHrOrAdmin()) {
            return true;
        }
        return isManagerOf(applicantId);
    }

    public boolean canAccessPayslip(Long payslipId) {
        if (payslipId == null) {
            return false;
        }
        if (currentUserService.isHrOrAdmin()) {
            return true;
        }
        Long currentEmpId = currentUserService.getCurrentEmployeeId().orElse(null);
        if (currentEmpId == null) {
            return false;
        }
        return payslipRepository.findById(payslipId)
                .map(p -> p.getEmployee() != null && currentEmpId.equals(p.getEmployee().getId()))
                .orElse(false);
    }

    public boolean isNotificationOwner(Long notificationId) {
        if (notificationId == null) {
            return false;
        }
        Long currentUserId = currentUserService.getCurrentUser().map(User::getId).orElse(null);
        if (currentUserId == null) {
            return false;
        }
        return notificationRepository.findById(notificationId)
                .map(n -> n.getRecipient() != null && currentUserId.equals(n.getRecipient().getId()))
                .orElse(false);
    }

    public boolean canSubmitSelfReview(Long reviewId) {
        if (reviewId == null) {
            return false;
        }
        Long currentEmpId = currentUserService.getCurrentEmployeeId().orElse(null);
        if (currentEmpId == null) {
            return false;
        }
        return performanceReviewRepository.findById(reviewId)
                .map(r -> r.getEmployee() != null && currentEmpId.equals(r.getEmployee().getId()))
                .orElse(false);
    }

    public boolean canSubmitManagerReview(Long reviewId) {
        if (reviewId == null) {
            return false;
        }
        Long currentEmpId = currentUserService.getCurrentEmployeeId().orElse(null);
        if (currentEmpId == null) {
            return false;
        }
        return performanceReviewRepository.findById(reviewId)
                .map(r -> {
                    if (r.getEmployee() == null) return false;
                    // Manager cannot review themselves
                    if (currentEmpId.equals(r.getEmployee().getId())) return false;
                    // Check if assigned manager or reporting manager
                    if (r.getManager() != null && currentEmpId.equals(r.getManager().getId())) return true;
                    return r.getEmployee().getReportingManager() != null &&
                            currentEmpId.equals(r.getEmployee().getReportingManager().getId());
                })
                .orElse(false);
    }
}
