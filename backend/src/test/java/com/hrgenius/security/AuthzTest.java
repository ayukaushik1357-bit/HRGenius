package com.hrgenius.security;

import com.hrgenius.entity.Document;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.User;
import com.hrgenius.entity.leave.LeaveApplication;
import com.hrgenius.entity.performance.PerformanceReview;
import com.hrgenius.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthzTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private PayslipRepository payslipRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PerformanceReviewRepository performanceReviewRepository;

    @InjectMocks
    private Authz authz;

    private Employee employee;
    private Employee manager;
    private Employee otherEmployee;

    @BeforeEach
    void setUp() {
        manager = new Employee();
        manager.setId(10L);

        employee = new Employee();
        employee.setId(20L);
        employee.setReportingManager(manager);

        otherEmployee = new Employee();
        otherEmployee.setId(30L);
    }

    @Test
    void testIsSelf() {
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(20L));
        assertTrue(authz.isSelf(20L));
        assertFalse(authz.isSelf(99L));
        assertFalse(authz.isSelf(null));
    }

    @Test
    void testIsManagerOf() {
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(10L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(30L)).thenReturn(Optional.of(otherEmployee));

        assertTrue(authz.isManagerOf(20L));
        assertFalse(authz.isManagerOf(30L));
        assertFalse(authz.isManagerOf(null));
    }

    @Test
    void testCanViewEmployee_AdminOrHr() {
        when(currentUserService.isHrOrAdmin()).thenReturn(true);
        assertTrue(authz.canViewEmployee(30L));
    }

    @Test
    void testCanViewEmployee_SelfOrManager() {
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(20L));

        assertTrue(authz.canViewEmployee(20L)); // self

        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(10L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        assertTrue(authz.canViewEmployee(20L)); // manager

        when(employeeRepository.findById(30L)).thenReturn(Optional.of(otherEmployee));
        assertFalse(authz.canViewEmployee(30L)); // unrelated
    }

    @Test
    void testCanAccessDocument() {
        Document doc = new Document();
        doc.setId(100L);
        doc.setEmployee(employee);

        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(20L));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        assertTrue(authz.canAccessDocument(100L));

        // Different employee
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(99L));
        assertFalse(authz.canAccessDocument(100L));
    }

    @Test
    void testCanApproveLeave_NobodyMayApproveOwnLeave() {
        LeaveApplication leave = new LeaveApplication();
        leave.setId(50L);
        leave.setEmployee(employee); // employee id 20

        when(leaveApplicationRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(20L));

        // Self attempting to approve own leave
        assertFalse(authz.canApproveLeave(50L));
    }

    @Test
    void testCanApproveLeave_ManagerCanApproveReportLeave() {
        LeaveApplication leave = new LeaveApplication();
        leave.setId(50L);
        leave.setEmployee(employee); // employee id 20

        when(leaveApplicationRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(10L)); // manager
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));

        assertTrue(authz.canApproveLeave(50L));
    }

    @Test
    void testCanSubmitSelfReview() {
        PerformanceReview review = new PerformanceReview();
        review.setId(200L);
        review.setEmployee(employee); // id 20

        when(performanceReviewRepository.findById(200L)).thenReturn(Optional.of(review));

        // Self review by employee
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(20L));
        assertTrue(authz.canSubmitSelfReview(200L));

        // Other employee attempting self review
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(30L));
        assertFalse(authz.canSubmitSelfReview(200L));
    }

    @Test
    void testCanSubmitManagerReview() {
        PerformanceReview review = new PerformanceReview();
        review.setId(200L);
        review.setEmployee(employee); // id 20
        review.setManager(manager); // id 10

        when(performanceReviewRepository.findById(200L)).thenReturn(Optional.of(review));

        // Direct manager
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(10L));
        assertTrue(authz.canSubmitManagerReview(200L));

        // Employee reviewing themselves as manager (forbidden)
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(20L));
        assertFalse(authz.canSubmitManagerReview(200L));

        // Unrelated employee
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(30L));
        assertFalse(authz.canSubmitManagerReview(200L));
    }

    @Test
    void testCanSubmitPeerFeedback() {
        PerformanceReview review = new PerformanceReview();
        review.setId(200L);
        review.setEmployee(employee); // id 20

        when(performanceReviewRepository.findById(200L)).thenReturn(Optional.of(review));

        // Peer employee (id 30) can submit feedback for employee (id 20)
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(30L));
        assertTrue(authz.canSubmitPeerFeedback(200L));

        // Cannot submit feedback for oneself
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(20L));
        assertFalse(authz.canSubmitPeerFeedback(200L));
    }
}
