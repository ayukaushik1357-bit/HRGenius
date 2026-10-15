package com.hrgenius.service;

import com.hrgenius.dto.offboarding.ExitRequestDto;
import com.hrgenius.dto.offboarding.FnFSettlementDto;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.leave.LeaveBalance;
import com.hrgenius.entity.offboarding.ExitRequest;
import com.hrgenius.entity.offboarding.FnFSettlement;
import com.hrgenius.entity.payroll.SalaryStructure;
import com.hrgenius.mapper.OffboardingMapper;
import com.hrgenius.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffboardingServiceTest {

    @Mock
    private ExitRequestRepository exitRequestRepository;
    @Mock
    private ClearanceItemRepository clearanceItemRepository;
    @Mock
    private FnFSettlementRepository fnfSettlementRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SalaryStructureRepository salaryStructureRepository;
    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OffboardingMapper offboardingMapper;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OffboardingService offboardingService;

    private Employee employee;
    private ExitRequest exitRequest;
    private SalaryStructure salaryStructure;
    private LeaveBalance leaveBalance;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(10L);
        employee.setEmployeeCode("EMP010");
        employee.setFirstName("Pooja");
        employee.setLastName("Patel");
        employee.setJoiningDate(LocalDate.of(2018, 1, 1)); // 7+ years of service
        employee.setStatus(Employee.EmployeeStatus.ACTIVE);

        exitRequest = new ExitRequest();
        exitRequest.setId(5L);
        exitRequest.setEmployee(employee);
        exitRequest.setResignationDate(LocalDate.of(2025, 4, 1));
        exitRequest.setNoticePeriodDays(30);
        exitRequest.setLastWorkingDay(LocalDate.of(2025, 4, 30));
        exitRequest.setStatus(ExitRequest.ExitStatus.PENDING);

        salaryStructure = new SalaryStructure();
        salaryStructure.setEmployee(employee);
        salaryStructure.setBasicSalary(BigDecimal.valueOf(52000.00));
        salaryStructure.setHra(BigDecimal.valueOf(26000.00));
        salaryStructure.setSpecialAllowance(BigDecimal.valueOf(10000.00));
        salaryStructure.setOtherAllowances(BigDecimal.valueOf(2000.00));

        leaveBalance = new LeaveBalance();
        leaveBalance.setEmployee(employee);
        leaveBalance.setEarnedQuota(15.0);
        leaveBalance.setEarnedUsed(5.0); // 10 days remaining
    }

    @Test
    void testCalculateFnFSettlementWithGratuity() {
        when(salaryStructureRepository.findByEmployeeId(10L)).thenReturn(Optional.of(salaryStructure));
        when(leaveBalanceRepository.findByEmployeeIdAndYear(10L, 2025)).thenReturn(Optional.of(leaveBalance));
        when(fnfSettlementRepository.findByExitRequestId(5L)).thenReturn(Optional.empty());
        when(fnfSettlementRepository.save(any(FnFSettlement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(offboardingMapper.toSettlementResponse(any(FnFSettlement.class))).thenAnswer(inv -> {
            FnFSettlement fnf = inv.getArgument(0);
            return FnFSettlementDto.Response.builder()
                    .id(fnf.getId())
                    .salaryPayable(fnf.getSalaryPayable())
                    .leaveEncashmentAmount(fnf.getLeaveEncashmentAmount())
                    .gratuityAmount(fnf.getGratuityAmount())
                    .netPayableAmount(fnf.getNetPayableAmount())
                    .status(fnf.getStatus().name())
                    .build();
        });

        FnFSettlementDto.Response res = offboardingService.calculateAndSaveFnF(exitRequest);

        assertNotNull(res);
        assertEquals("DRAFT", res.getStatus());
        // Basic: 52000, dailyBasic = 52000 / 30 = 1733.33. 10 days encashment = 17333.30
        assertTrue(res.getLeaveEncashmentAmount().compareTo(BigDecimal.ZERO) > 0);
        // Tenure 7 years -> gratuity eligible!
        assertTrue(res.getGratuityAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(res.getNetPayableAmount().compareTo(BigDecimal.ZERO) > 0);
    }
}
