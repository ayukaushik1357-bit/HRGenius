package com.hrgenius.service;

import com.hrgenius.dto.common.PagedResponse;
import com.hrgenius.dto.employee.EmployeeRequest;
import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.entity.Address;
import com.hrgenius.entity.Department;
import com.hrgenius.entity.Designation;
import com.hrgenius.entity.EmergencyContact;
import com.hrgenius.entity.Employee;
import com.hrgenius.exception.DuplicateResourceException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.EmployeeMapper;
import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import com.hrgenius.repository.DepartmentRepository;
import com.hrgenius.repository.DesignationRepository;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.RoleRepository;
import com.hrgenius.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeMapper employeeMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> searchEmployees(
            String name,
            Long departmentId,
            Employee.EmployeeStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                String pattern = "%" + name.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("employeeCode")), pattern, '\\')
                ));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);

        List<EmployeeResponse> content = employeePage.getContent().stream()
                .map(employeeMapper::toListResponse)
                .toList();

        return PagedResponse.<EmployeeResponse>builder()
                .content(content)
                .page(employeePage.getNumber())
                .size(employeePage.getSize())
                .totalElements(employeePage.getTotalElements())
                .totalPages(employeePage.getTotalPages())
                .last(employeePage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee with email '" + request.getEmail() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }

        Employee employee = employeeMapper.toEntity(request);
        String employeeCode = generateNextEmployeeCode();
        employee.setEmployeeCode(employeeCode);

        // Create linked User with role EMPLOYEE and temporary password (must-change flag on)
        Role employeeRole = roleRepository.findByName(Role.RoleName.EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", Role.RoleName.EMPLOYEE));

        String username = employeeCode.toLowerCase();
        if (userRepository.existsByUsername(username)) {
            username = request.getEmail().split("@")[0].toLowerCase();
            if (userRepository.existsByUsername(username)) {
                username = employeeCode.toLowerCase() + "_" + (System.currentTimeMillis() % 1000);
            }
        }
        String tempPassword = "Temp@" + employeeCode + "!1";

        User user = new User();
        user.setUsername(username);
        user.setEmail(employee.getEmail());
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setEnabled(true);
        user.setMustChangePassword(true);
        user.setRoles(Collections.singleton(employeeRole));
        User savedUser = userRepository.save(user);

        employee.setUser(savedUser);

        resolveRelationships(employee, request);
        resolveAddressesAndContacts(employee, request);

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        employeeRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Employee with email '" + request.getEmail() + "' already exists");
            }
        });

        employeeMapper.updateEntity(employee, request);
        resolveRelationships(employee, request);

        // Update addresses and contacts
        employee.getAddresses().clear();
        employee.getEmergencyContacts().clear();
        resolveAddressesAndContacts(employee, request);

        Employee updated = employeeRepository.save(employee);
        return employeeMapper.toResponse(updated);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", "id", id);
        }
        employeeRepository.deleteById(id);
    }

    private synchronized String generateNextEmployeeCode() {
        int maxCode = 0;
        try {
            maxCode = employeeRepository.findMaxEmployeeCodeNumber();
        } catch (Exception ignored) {
            // fallback if table is empty or first record
        }
        return String.format("EMP%04d", maxCode + 1);
    }

    private void resolveRelationships(Employee employee, EmployeeRequest request) {
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }

        if (request.getDesignationId() != null) {
            Designation designation = designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId()));
            employee.setDesignation(designation);
        } else {
            employee.setDesignation(null);
        }

        if (request.getReportingManagerId() != null) {
            if (employee.getId() != null && employee.getId().equals(request.getReportingManagerId())) {
                throw new IllegalArgumentException("An employee cannot be their own reporting manager");
            }
            Employee manager = employeeRepository.findById(request.getReportingManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reporting Manager", "id", request.getReportingManagerId()));
            employee.setReportingManager(manager);
        } else {
            employee.setReportingManager(null);
        }
    }

    private void resolveAddressesAndContacts(Employee employee, EmployeeRequest request) {
        if (request.getAddresses() != null) {
            for (EmployeeRequest.AddressDto addrDto : request.getAddresses()) {
                Address address = new Address();
                address.setAddressType(Address.AddressType.valueOf(addrDto.getAddressType().toUpperCase()));
                address.setLine1(addrDto.getLine1());
                address.setLine2(addrDto.getLine2());
                address.setCity(addrDto.getCity());
                address.setState(addrDto.getState());
                address.setCountry(addrDto.getCountry());
                address.setZipCode(addrDto.getZipCode());
                employee.addAddress(address);
            }
        }

        if (request.getEmergencyContacts() != null) {
            for (EmployeeRequest.EmergencyContactDto contactDto : request.getEmergencyContacts()) {
                EmergencyContact contact = new EmergencyContact();
                contact.setName(contactDto.getName());
                contact.setRelationship(contactDto.getRelationship());
                contact.setPhone(contactDto.getPhone());
                employee.addEmergencyContact(contact);
            }
        }
    }
}
