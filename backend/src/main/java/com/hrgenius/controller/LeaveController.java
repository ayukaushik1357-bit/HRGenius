package com.hrgenius.controller;

import com.hrgenius.dto.leave.LeaveApplicationRequest;
import com.hrgenius.dto.leave.LeaveApplicationResponse;
import com.hrgenius.dto.leave.LeaveApprovalRequest;
import com.hrgenius.dto.leave.LeaveBalanceResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.leave.LeaveApplication;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.security.CurrentUserService;
import com.hrgenius.security.UserPrincipal;
import com.hrgenius.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/leaves")
@RequiredArgsConstructor
@Tag(name = "Leave Management", description = "Leave applications, balances, and manager approval inbox")
public class LeaveController {

    private final LeaveService leaveService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Apply for leave")
    public ResponseEntity<LeaveApplicationResponse> applyLeave(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long employeeId,
            @Valid @RequestBody LeaveApplicationRequest request) {
        Long empId = currentUserService.getEmployeeIdForUser(principal, employeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.applyLeave(empId, request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get leave applications for authenticated employee")
    public ResponseEntity<List<LeaveApplicationResponse>> getMyLeaves(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long employeeId) {
        Long empId = currentUserService.getEmployeeIdForUser(principal, employeeId);
        return ResponseEntity.ok(leaveService.getEmployeeLeaves(empId));
    }

    @GetMapping("/balance")
    @Operation(summary = "Get leave quota and balance for employee")
    public ResponseEntity<LeaveBalanceResponse> getLeaveBalance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year) {
        Long empId = currentUserService.getEmployeeIdForUser(principal, employeeId);
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(leaveService.getLeaveBalance(empId, targetYear));
    }

    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get pending leave applications waiting for manager approval")
    public ResponseEntity<List<LeaveApplicationResponse>> getPendingApprovals(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long managerEmpId = currentUserService.getCurrentEmployeeId().orElse(null);
        if (managerEmpId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(leaveService.getPendingApprovals(managerEmpId));
    }

    @PostMapping("/{id}/action")
    @PreAuthorize("@authz.canApproveLeave(#id)")
    @Operation(summary = "Approve or reject a leave application")
    public ResponseEntity<LeaveApplicationResponse> actionLeave(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody LeaveApprovalRequest request) {
        Long approverEmpId = currentUserService.getCurrentEmployeeId().orElse(null);
        return ResponseEntity.ok(leaveService.processLeaveApproval(id, approverEmpId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "List all leave applications across company (ADMIN/HR_MANAGER)")
    public ResponseEntity<List<LeaveApplicationResponse>> getAllLeaves(
            @RequestParam(required = false) LeaveApplication.LeaveStatus status) {
        return ResponseEntity.ok(leaveService.getAllLeaves(status));
    }
}
