package com.hrgenius.mapper;

import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeMapperTest {

    @Mock
    private CurrentUserService currentUserService;

    private EmployeeMapper employeeMapper;
    private Employee employee;

    @BeforeEach
    void setUp() {
        employeeMapper = new EmployeeMapper(currentUserService);

        employee = new Employee();
        employee.setId(10L);
        employee.setEmployeeCode("EMP010");
        employee.setFirstName("Rohan");
        employee.setLastName("Sharma");
        employee.setEmail("rohan@hrgenius.com");
        employee.setSalaryCTC(BigDecimal.valueOf(1200000.00));
        employee.setPanNumber("ABCDE1234F");
        employee.setAadhaarNumber("123456789012");
        employee.setBankAccountNumber("9876543210123");
        employee.setBankIfscCode("HDFC0001234");
        employee.setBankName("HDFC Bank");
    }

    @Test
    void testToResponse_Admin_RevealsAllSensitiveFields() {
        when(currentUserService.isHrOrAdmin()).thenReturn(true);

        EmployeeResponse res = employeeMapper.toResponse(employee);

        assertEquals(BigDecimal.valueOf(1200000.00), res.getSalaryCTC());
        assertEquals("ABCDE1234F", res.getPanNumber());
        assertEquals("123456789012", res.getAadhaarNumber());
        assertEquals("9876543210123", res.getBankAccountNumber());
        assertEquals("HDFC0001234", res.getBankIfscCode());
        assertEquals("HDFC Bank", res.getBankName());
    }

    @Test
    void testToResponse_Self_RevealsAllSensitiveFields() {
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(10L)); // same as employee.id

        EmployeeResponse res = employeeMapper.toResponse(employee);

        assertEquals(BigDecimal.valueOf(1200000.00), res.getSalaryCTC());
        assertEquals("ABCDE1234F", res.getPanNumber());
        assertEquals("123456789012", res.getAadhaarNumber());
        assertEquals("9876543210123", res.getBankAccountNumber());
        assertEquals("HDFC0001234", res.getBankIfscCode());
        assertEquals("HDFC Bank", res.getBankName());
    }

    @Test
    void testToResponse_ManagerOrOther_MasksSensitiveFields() {
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentEmployeeId()).thenReturn(Optional.of(99L)); // manager id != 10

        EmployeeResponse res = employeeMapper.toResponse(employee);

        assertNull(res.getSalaryCTC(), "Salary should be null for manager/non-self");
        assertEquals("XXXXXX234F", res.getPanNumber());
        assertEquals("XXXX-XXXX-9012", res.getAadhaarNumber());
        assertEquals("XXXXXXXX0123", res.getBankAccountNumber());
        assertEquals("XXXX", res.getBankIfscCode());
        assertNull(res.getBankName());
    }
}
