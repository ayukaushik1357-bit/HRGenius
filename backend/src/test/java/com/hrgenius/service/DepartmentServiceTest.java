package com.hrgenius.service;

import com.hrgenius.dto.department.DepartmentRequest;
import com.hrgenius.dto.department.DepartmentResponse;
import com.hrgenius.entity.Department;
import com.hrgenius.exception.DuplicateResourceException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.DepartmentMapper;
import com.hrgenius.exception.ResourceInUseException;
import com.hrgenius.repository.DepartmentRepository;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Spy
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private DepartmentRequest request;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        department.setCode("ENG");
        department.setDescription("Software Engineering");

        request = new DepartmentRequest();
        request.setName("Engineering");
        request.setCode("ENG");
        request.setDescription("Software Engineering");
    }

    @Test
    void testGetAllDepartments() {
        when(departmentRepository.findAll()).thenReturn(List.of(department));

        List<DepartmentResponse> result = departmentService.getAllDepartments();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).getName());
        assertEquals("ENG", result.get(0).getCode());
    }

    @Test
    void testGetDepartmentById_Success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        DepartmentResponse response = departmentService.getDepartmentById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("ENG", response.getCode());
    }

    @Test
    void testGetDepartmentById_NotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartmentById(99L));
    }

    @Test
    void testCreateDepartment_Success() {
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentRepository.existsByCode("ENG")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department d = invocation.getArgument(0);
            d.setId(10L);
            return d;
        });

        DepartmentResponse created = departmentService.createDepartment(request);

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("Engineering", created.getName());
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void testCreateDepartment_DuplicateName() {
        when(departmentRepository.existsByName("Engineering")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    void testDeleteDepartment_Success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        departmentService.deleteDepartment(1L);

        verify(departmentRepository).delete(department);
    }

    @Test
    void testDeleteDepartment_StillHasEmployees() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.countActiveInDepartment(1L)).thenReturn(3L);

        ResourceInUseException ex = assertThrows(ResourceInUseException.class,
                () -> departmentService.deleteDepartment(1L));
        assertEquals("Engineering still has 3 employees. Move them to another department first.", ex.getMessage());
        verify(departmentRepository, never()).delete(any());
    }
}
