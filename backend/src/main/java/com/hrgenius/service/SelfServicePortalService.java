package com.hrgenius.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.dto.attendance.AttendanceResponse;
import com.hrgenius.dto.attendance.HolidayResponse;
import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.dto.leave.LeaveBalanceResponse;
import com.hrgenius.dto.payroll.PayslipResponse;
import com.hrgenius.dto.selfservice.PortalDashboardResponse;
import com.hrgenius.dto.selfservice.ProfileUpdateRequestDto;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.selfservice.ProfileUpdateRequest;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.AttendanceMapper;
import com.hrgenius.mapper.EmployeeMapper;
import com.hrgenius.mapper.LeaveMapper;
import com.hrgenius.mapper.PayrollMapper;
import com.hrgenius.repository.*;
import com.hrgenius.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfServicePortalService {

    private final EmployeeRepository employeeRepository;
    private final com.hrgenius.security.CurrentUserService currentUserService;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PayslipRepository payslipRepository;
    private final NotificationRepository notificationRepository;
    private final OnboardingTaskRepository onboardingTaskRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final ProfileUpdateRequestRepository profileUpdateRequestRepository;
    private final EmployeeMapper employeeMapper;
    private final AttendanceMapper attendanceMapper;
    private final LeaveMapper leaveMapper;
    private final PayrollMapper payrollMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PortalDashboardResponse getPortalDashboard() {
        Employee emp = getCurrentEmployee();

        // 1. Profile
        EmployeeResponse profile = employeeMapper.toResponse(emp);

        // 2. Today's Attendance
        LocalDate today = LocalDate.now();
        AttendanceResponse todayAtt = attendanceRepository.findByEmployeeIdAndDate(emp.getId(), today)
                .map(attendanceMapper::toAttendanceResponse)
                .orElse(null);

        // 3. Leave Balance
        LeaveBalanceResponse leaveBal = leaveBalanceRepository.findByEmployeeIdAndYear(emp.getId(), today.getYear())
                .map(leaveMapper::toBalanceResponse)
                .orElse(null);

        // 4. Latest Payslip
        List<com.hrgenius.entity.payroll.Payslip> payslips = payslipRepository.findByEmployeeIdOrderByYearDescMonthDesc(emp.getId());
        PayslipResponse latestPayslip = payslips.isEmpty() ? null : payrollMapper.toPayslipResponse(payslips.get(0));

        // 5. Upcoming Holidays
        List<HolidayResponse> upcomingHolidays = holidayRepository.findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(today).stream()
                .limit(5)
                .map(attendanceMapper::toHolidayResponse)
                .toList();

        // 6. Pending Tasks
        long pendingOnboarding = onboardingTaskRepository.countByEmployeeIdAndStatus(
                emp.getId(), com.hrgenius.entity.onboarding.OnboardingTask.TaskStatus.PENDING);

        long pendingReviews = performanceReviewRepository.findByEmployeeId(emp.getId()).stream()
                .filter(r -> r.getStatus() == com.hrgenius.entity.performance.PerformanceReview.ReviewStatus.SELF_REVIEW
                        || r.getStatus() == com.hrgenius.entity.performance.PerformanceReview.ReviewStatus.NOT_STARTED)
                .count();

        long totalPendingTasks = pendingOnboarding + pendingReviews;

        // 7. Unread Notifications
        long unreadNotifications = 0;
        if (emp.getUser() != null) {
            unreadNotifications = notificationRepository.countByRecipientUsernameAndReadFalse(emp.getUser().getUsername());
        }

        return PortalDashboardResponse.builder()
                .profile(profile)
                .todayAttendance(todayAtt)
                .leaveBalance(leaveBal)
                .latestPayslip(latestPayslip)
                .upcomingHolidays(upcomingHolidays)
                .pendingTasksCount(totalPendingTasks)
                .unreadNotificationsCount(unreadNotifications)
                .build();
    }

    @Transactional
    public ProfileUpdateRequestDto.Response submitProfileUpdateRequest(ProfileUpdateRequestDto.Request req) {
        Employee emp = getCurrentEmployee();

        ProfileUpdateRequest pur = new ProfileUpdateRequest();
        pur.setEmployee(emp);
        pur.setRequestedChanges(req.getRequestedChanges());
        pur.setStatus(ProfileUpdateRequest.RequestStatus.PENDING);

        ProfileUpdateRequest saved = profileUpdateRequestRepository.save(pur);

        if (emp.getUser() != null) {
            notificationService.sendNotification(
                    emp.getUser().getId(),
                    "Profile Update Submitted",
                    "Your request to update profile details has been submitted for HR review.",
                    "PROFILE_UPDATE"
            );
        }

        return toResponse(saved);
    }

    @Transactional
    public ProfileUpdateRequestDto.Response reviewProfileUpdateRequest(Long requestId, ProfileUpdateRequestDto.ActionRequest action) {
        ProfileUpdateRequest pur = profileUpdateRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile update request not found with id: " + requestId));

        Employee currentEmp = getCurrentEmployee();
        pur.setReviewedBy(currentEmp);
        pur.setReviewedAt(LocalDateTime.now());
        pur.setRemarks(action.getRemarks());

        if (action.isApproved()) {
            pur.setStatus(ProfileUpdateRequest.RequestStatus.APPROVED);

            // Apply updates to employee entity
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> changes = objectMapper.readValue(pur.getRequestedChanges(), Map.class);
                Employee emp = pur.getEmployee();

                if (changes.containsKey("phone")) {
                    String oldPhone = emp.getPhone();
                    emp.setPhone((String) changes.get("phone"));
                    auditService.logChange("Employee", emp.getId(), "UPDATE_PHONE", oldPhone, emp.getPhone());
                }
                if (changes.containsKey("bankAccountNumber")) {
                    String oldAcc = emp.getBankAccountNumber();
                    emp.setBankAccountNumber((String) changes.get("bankAccountNumber"));
                    auditService.logChange("Employee", emp.getId(), "UPDATE_BANK_ACC", oldAcc, emp.getBankAccountNumber());
                }
                if (changes.containsKey("bankIfscCode")) {
                    emp.setBankIfscCode((String) changes.get("bankIfscCode"));
                }
                if (changes.containsKey("bankName")) {
                    emp.setBankName((String) changes.get("bankName"));
                }
                if (changes.containsKey("panNumber")) {
                    emp.setPanNumber((String) changes.get("panNumber"));
                }
                if (changes.containsKey("aadhaarNumber")) {
                    emp.setAadhaarNumber((String) changes.get("aadhaarNumber"));
                }

                employeeRepository.save(emp);
            } catch (Exception e) {
                log.warn("Could not dynamically apply profile changes JSON: {}", e.getMessage());
            }

            if (pur.getEmployee().getUser() != null) {
                notificationService.sendNotification(
                        pur.getEmployee().getUser().getId(),
                        "Profile Update Approved",
                        "Your profile updates have been approved and applied.",
                        "PROFILE_UPDATE"
                );
            }
        } else {
            pur.setStatus(ProfileUpdateRequest.RequestStatus.REJECTED);

            if (pur.getEmployee().getUser() != null) {
                notificationService.sendNotification(
                        pur.getEmployee().getUser().getId(),
                        "Profile Update Rejected",
                        "Your profile update request was rejected: " + action.getRemarks(),
                        "PROFILE_UPDATE"
                );
            }
        }

        return toResponse(profileUpdateRequestRepository.save(pur));
    }

    @Transactional(readOnly = true)
    public List<ProfileUpdateRequestDto.Response> getAllPendingRequests() {
        return profileUpdateRequestRepository.findByStatusOrderByCreatedAtDesc(ProfileUpdateRequest.RequestStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfileUpdateRequestDto.Response> getMyRequests() {
        Employee emp = getCurrentEmployee();
        return profileUpdateRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(emp.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProfileUpdateRequestDto.Response toResponse(ProfileUpdateRequest pur) {
        return ProfileUpdateRequestDto.Response.builder()
                .id(pur.getId())
                .employeeId(pur.getEmployee() != null ? pur.getEmployee().getId() : null)
                .employeeName(pur.getEmployee() != null ? pur.getEmployee().getFirstName() + " " + pur.getEmployee().getLastName() : null)
                .employeeCode(pur.getEmployee() != null ? pur.getEmployee().getEmployeeCode() : null)
                .requestedChanges(pur.getRequestedChanges())
                .status(pur.getStatus() != null ? pur.getStatus().name() : null)
                .reviewedById(pur.getReviewedBy() != null ? pur.getReviewedBy().getId() : null)
                .reviewedByName(pur.getReviewedBy() != null ? pur.getReviewedBy().getFirstName() + " " + pur.getReviewedBy().getLastName() : null)
                .reviewedAt(pur.getReviewedAt())
                .remarks(pur.getRemarks())
                .createdAt(pur.getCreatedAt())
                .build();
    }

    /**
     * The signed-in user's own employee record. Never falls back to another employee:
     * a user without a linked record gets a clear 404 instead of someone else's data.
     */
    private Employee getCurrentEmployee() {
        return currentUserService.getCurrentEmployeeOrThrow();
    }
}
