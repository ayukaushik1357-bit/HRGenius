package com.hrgenius.service;

import com.hrgenius.dto.leave.LeaveApplicationRequest;
import com.hrgenius.dto.leave.LeaveApplicationResponse;
import com.hrgenius.dto.leave.LeaveApprovalRequest;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.leave.LeaveApplication;
import com.hrgenius.entity.leave.LeaveBalance;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.mapper.LeaveMapper;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.LeaveApplicationRepository;
import com.hrgenius.repository.LeaveBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveMapper leaveMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.hrgenius.security.CurrentUserService currentUserService;

    @InjectMocks
    private LeaveService leaveService;

    private Employee employee;
    private LeaveBalance balance;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Alice");
        employee.setLastName("Smith");

        balance = new LeaveBalance();
        balance.setId(10L);
        balance.setEmployee(employee);
        balance.setYear(2026);
        balance.setCasualQuota(12.0);
        balance.setCasualUsed(2.0); // 10 remaining
        balance.setSickQuota(10.0);
        balance.setSickUsed(0.0);   // 10 remaining
        balance.setEarnedQuota(15.0);
        balance.setEarnedUsed(5.0); // 10 remaining
    }

    @Test
    @DisplayName("Should successfully apply for leave when balance is sufficient")
    void applyLeave_successful() {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setLeaveType("CASUAL");
        request.setStartDate(LocalDate.of(2026, 4, 10));
        request.setEndDate(LocalDate.of(2026, 4, 11));
        request.setNumberOfDays(2.0);
        request.setReason("Personal work");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveApplicationRepository.countOverlappingLeaves(eq(1L), any(), any(), isNull())).thenReturn(0L);
        when(leaveBalanceRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(Optional.of(balance));

        LeaveApplication savedApp = new LeaveApplication();
        savedApp.setId(100L);
        savedApp.setStatus(LeaveApplication.LeaveStatus.PENDING);
        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenReturn(savedApp);

        LeaveApplicationResponse expectedResponse = LeaveApplicationResponse.builder()
                .id(100L)
                .status("PENDING")
                .leaveType("CASUAL")
                .numberOfDays(2.0)
                .build();
        when(leaveMapper.toApplicationResponse(savedApp)).thenReturn(expectedResponse);

        LeaveApplicationResponse response = leaveService.applyLeave(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(leaveApplicationRepository).save(any(LeaveApplication.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when requested leave exceeds balance")
    void applyLeave_insufficientBalance_throwsException() {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setLeaveType("CASUAL");
        request.setStartDate(LocalDate.of(2026, 4, 10));
        request.setEndDate(LocalDate.of(2026, 4, 25));
        request.setNumberOfDays(15.0); // only 10 available
        request.setReason("Long vacation");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveApplicationRepository.countOverlappingLeaves(eq(1L), any(), any(), isNull())).thenReturn(0L);
        when(leaveBalanceRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> leaveService.applyLeave(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient Casual leave balance");

        verify(leaveApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when leave overlaps with existing application")
    void applyLeave_overlapping_throwsException() {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setLeaveType("CASUAL");
        request.setStartDate(LocalDate.of(2026, 4, 10));
        request.setEndDate(LocalDate.of(2026, 4, 12));
        request.setNumberOfDays(3.0);
        request.setReason("Doctor visit");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveApplicationRepository.countOverlappingLeaves(eq(1L), any(), any(), isNull())).thenReturn(1L);

        assertThatThrownBy(() -> leaveService.applyLeave(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("overlapping");

        verify(leaveApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should deduct quota upon manager approval of direct report")
    void processLeaveApproval_approved_deductsQuota() {
        Employee manager = new Employee();
        manager.setId(2L);
        employee.setReportingManager(manager);

        LeaveApplication app = new LeaveApplication();
        app.setId(100L);
        app.setEmployee(employee);
        app.setLeaveType(LeaveApplication.LeaveType.CASUAL);
        app.setStartDate(LocalDate.of(2026, 5, 2));
        app.setEndDate(LocalDate.of(2026, 5, 3));
        app.setNumberOfDays(2.0);
        app.setStatus(LeaveApplication.LeaveStatus.PENDING);

        when(leaveApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(leaveBalanceRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(Optional.of(balance));
        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenReturn(app);
        when(currentUserService.isHrOrAdmin()).thenReturn(false);

        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest();
        approvalRequest.setApproved(true);

        leaveService.processLeaveApproval(100L, 2L, approvalRequest);

        assertThat(app.getStatus()).isEqualTo(LeaveApplication.LeaveStatus.APPROVED);
        assertThat(balance.getCasualUsed()).isEqualTo(4.0); // 2.0 previous + 2.0 new
        verify(leaveBalanceRepository).save(balance);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when employee tries to approve own leave")
    void processLeaveApproval_selfApproval_throwsAccessDenied() {
        LeaveApplication app = new LeaveApplication();
        app.setId(100L);
        app.setEmployee(employee); // employee.id is 1L
        app.setStatus(LeaveApplication.LeaveStatus.PENDING);

        when(leaveApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest();
        approvalRequest.setApproved(true);

        assertThatThrownBy(() -> leaveService.processLeaveApproval(100L, 1L, approvalRequest))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Cannot approve or reject your own leave application");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when manager attempts to approve non-direct report")
    void processLeaveApproval_nonReportingManager_throwsAccessDenied() {
        Employee manager = new Employee();
        manager.setId(99L);
        employee.setReportingManager(manager); // reporting manager is 99L

        LeaveApplication app = new LeaveApplication();
        app.setId(100L);
        app.setEmployee(employee);
        app.setStatus(LeaveApplication.LeaveStatus.PENDING);

        when(leaveApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(currentUserService.isHrOrAdmin()).thenReturn(false);

        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest();
        approvalRequest.setApproved(true);

        // Approver is 2L, but reporting manager is 99L
        assertThatThrownBy(() -> leaveService.processLeaveApproval(100L, 2L, approvalRequest))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Managers can only approve or reject leaves for their direct reports");
    }

    @Test
    @DisplayName("Should allow HR/Admin to approve any employee's leave")
    void processLeaveApproval_adminApproval_succeeds() {
        LeaveApplication app = new LeaveApplication();
        app.setId(100L);
        app.setEmployee(employee);
        app.setLeaveType(LeaveApplication.LeaveType.CASUAL);
        app.setStartDate(LocalDate.of(2026, 5, 2));
        app.setEndDate(LocalDate.of(2026, 5, 3));
        app.setNumberOfDays(2.0);
        app.setStatus(LeaveApplication.LeaveStatus.PENDING);

        when(leaveApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(leaveBalanceRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(Optional.of(balance));
        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenReturn(app);
        when(currentUserService.isHrOrAdmin()).thenReturn(true);

        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest();
        approvalRequest.setApproved(true);

        LeaveApplicationResponse response = leaveService.processLeaveApproval(100L, 50L, approvalRequest);
        assertThat(app.getStatus()).isEqualTo(LeaveApplication.LeaveStatus.APPROVED);
    }
}
