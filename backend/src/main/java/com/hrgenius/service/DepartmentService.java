package com.hrgenius.service;

import com.hrgenius.dto.department.DepartmentRequest;
import com.hrgenius.dto.department.DepartmentResponse;
import com.hrgenius.entity.Department;
import com.hrgenius.exception.DuplicateResourceException;
import com.hrgenius.exception.ResourceInUseException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.DepartmentMapper;
import com.hrgenius.repository.DepartmentRepository;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final EmployeeRepository employeeRepository;
    private final JobPostingRepository jobPostingRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return departmentMapper.toResponse(department);
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department with name '" + request.getName() + "' already exists");
        }
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Department with code '" + request.getCode() + "' already exists");
        }

        Department department = departmentMapper.toEntity(request);
        Department saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        departmentRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Department with name '" + request.getName() + "' already exists");
            }
        });

        departmentRepository.findByCode(request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Department with code '" + request.getCode() + "' already exists");
            }
        });

        departmentMapper.updateEntity(department, request);
        return departmentMapper.toResponse(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        long people = employeeRepository.countActiveInDepartment(id);
        if (people > 0) {
            throw new ResourceInUseException(department.getName() + " still has " + people + (people == 1 ? " employee" : " employees") + ". Move them to another department first.");
        }
        long postings = jobPostingRepository.countByDepartmentId(id);
        if (postings > 0) {
            throw new ResourceInUseException(department.getName() + " is used by " + postings
                    + (postings == 1 ? " job posting" : " job postings") + ". Change or delete them first.");
        }
        employeeRepository.detachDeletedFromDepartment(id);
        departmentRepository.delete(department);
    }
}
