package com.hrgenius.service;

import com.hrgenius.dto.employee.EmployeeRequest;
import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.entity.Department;
import com.hrgenius.entity.Designation;
import com.hrgenius.entity.Employee;
import com.hrgenius.exception.DuplicateResourceException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.EmployeeMapper;
import com.hrgenius.repository.DepartmentRepository;
import com.hrgenius.repository.DesignationRepository;
import com.hrgenius.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DesignationRepository designationRepository;

    @Mock
    private com.hrgenius.repository.UserRepository userRepository;

    @Mock
    private com.hrgenius.repository.RoleRepository roleRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private com.hrgenius.security.CurrentUserService currentUserService;

    @Spy
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;
    private EmployeeRequest request;
    private Department department;
    private Designation designation;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Engineering");

        designation = new Designation();
        designation.setId(1L);
        designation.setTitle("Software Engineer");

        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("EMP0001");
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe@hrgenius.com");
        employee.setJoiningDate(LocalDate.of(2025, 1, 15));
        employee.setDepartment(department);
        employee.setDesignation(designation);
        employee.setStatus(Employee.EmployeeStatus.ACTIVE);
        employee.setEmploymentType(Employee.EmploymentType.FULL_TIME);
        employee.setSalaryCTC(BigDecimal.valueOf(1200000));

        request = new EmployeeRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@hrgenius.com");
        request.setJoiningDate(LocalDate.of(2025, 1, 15));
        request.setDepartmentId(1L);
        request.setDesignationId(1L);
        request.setStatus(Employee.EmployeeStatus.ACTIVE);
        request.setEmploymentType(Employee.EmploymentType.FULL_TIME);
        request.setSalaryCTC(BigDecimal.valueOf(1200000));
    }

    @Test
    void testGetEmployeeById_Success() {
        when(employeeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertNotNull(response);
        assertEquals("EMP0001", response.getEmployeeCode());
        assertEquals("John", response.getFirstName());
        assertEquals("john.doe@hrgenius.com", response.getEmail());
    }

    @Test
    void testGetEmployeeById_NotFound() {
        when(employeeRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(99L));
    }

    @Test
    void testCreateEmployee_Success() {
        when(employeeRepository.existsByEmail("john.doe@hrgenius.com")).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("john.doe@hrgenius.com")).thenReturn(Optional.empty());
        when(employeeRepository.findMaxEmployeeCodeNumber()).thenReturn(0);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));

        com.hrgenius.entity.Role employeeRole = new com.hrgenius.entity.Role(com.hrgenius.entity.Role.RoleName.EMPLOYEE);
        when(roleRepository.findByName(com.hrgenius.entity.Role.RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(userRepository.existsByUsername("emp0001")).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedTempPassword");
        when(userRepository.save(any(com.hrgenius.entity.User.class))).thenAnswer(inv -> {
            com.hrgenius.entity.User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee e = invocation.getArgument(0);
            e.setId(5L);
            return e;
        });

        EmployeeResponse created = employeeService.createEmployee(request);

        assertNotNull(created);
        assertEquals("EMP0001", created.getEmployeeCode());
        assertEquals("John", created.getFirstName());
        verify(employeeRepository).save(any(Employee.class));
        org.mockito.ArgumentCaptor<com.hrgenius.entity.User> userCaptor = org.mockito.ArgumentCaptor.forClass(com.hrgenius.entity.User.class);
        verify(userRepository).save(userCaptor.capture());
        com.hrgenius.entity.User capturedUser = userCaptor.getValue();
        assertEquals("emp0001", capturedUser.getUsername());
        assertEquals("john.doe@hrgenius.com", capturedUser.getEmail());
        org.junit.jupiter.api.Assertions.assertTrue(capturedUser.isMustChangePassword());
        org.junit.jupiter.api.Assertions.assertTrue(capturedUser.isEnabled());
    }

    @Test
    void testCreateEmployee_DuplicateEmail() {
        when(employeeRepository.existsByEmail("john.doe@hrgenius.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchEmployees() {
        org.springframework.data.domain.Page<Employee> page =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(employee));
        when(employeeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        com.hrgenius.dto.common.PagedResponse<EmployeeResponse> result = employeeService.searchEmployees(
                "John", null, null, 0, 10, "id", "asc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("EMP0001", result.getContent().get(0).getEmployeeCode());
    }
}
