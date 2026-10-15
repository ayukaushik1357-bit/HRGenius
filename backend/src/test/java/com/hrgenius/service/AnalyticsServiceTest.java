package com.hrgenius.service;

import com.hrgenius.dto.analytics.AnalyticsDashboardResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.payroll.PayrollRun;
import com.hrgenius.entity.recruitment.JobPosting;
import com.hrgenius.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private PayslipRepository payslipRepository;
    @Mock
    private PerformanceReviewRepository performanceReviewRepository;
    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;
    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private ExitRequestRepository exitRequestRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        when(employeeRepository.countByStatus(Employee.EmployeeStatus.ACTIVE)).thenReturn(45L);
        when(employeeRepository.countByStatus(Employee.EmployeeStatus.EXITED)).thenReturn(5L);
        when(employeeRepository.countByStatus(Employee.EmployeeStatus.ON_NOTICE)).thenReturn(2L);
        when(employeeRepository.getHeadcountByDepartment()).thenReturn(List.<Object[]>of(
                new Object[]{"Engineering", 25L},
                new Object[]{"Finance", 10L}
        ));
        when(employeeRepository.getGenderDistribution()).thenReturn(List.<Object[]>of(
                new Object[]{"MALE", 30L},
                new Object[]{"FEMALE", 22L}
        ));
        when(applicationRepository.getRecruitmentFunnel()).thenReturn(List.<Object[]>of(
                new Object[]{"APPLIED", 15L},
                new Object[]{"INTERVIEW", 6L}
        ));
        when(payslipRepository.getPayrollCostByDepartment(anyInt(), anyInt())).thenReturn(List.<Object[]>of(
                new Object[]{"Engineering", BigDecimal.valueOf(1500000.00)}
        ));
        when(performanceReviewRepository.getRatingDistribution()).thenReturn(List.<Object[]>of(
                new Object[]{BigDecimal.valueOf(4.5), 10L}
        ));
        when(jobPostingRepository.findByStatus(any(JobPosting.JobPostingStatus.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void testGetDashboardData() {
        AnalyticsDashboardResponse response = analyticsService.getDashboardData("ADMIN", null);
        assertNotNull(response);
        assertEquals(52L, response.getTotalHeadcount());
        assertEquals(45L, response.getActiveEmployees());
        assertTrue(response.getAttritionRate() > 0);
        assertFalse(response.getHeadcountByDepartment().isEmpty());
        assertFalse(response.getGenderDistribution().isEmpty());
    }

    @Test
    void testExportExcel() {
        byte[] excelBytes = analyticsService.exportToExcel(2025, 4);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void testExportPdf() {
        byte[] pdfBytes = analyticsService.exportToPdf(2025, 4);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
