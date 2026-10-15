package com.hrgenius.repository;

import com.hrgenius.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmployeeCode(String employeeCode);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN FETCH e.department " +
           "LEFT JOIN FETCH e.designation " +
           "WHERE e.id = :id")
    Optional<Employee> findByIdWithDetails(@Param("id") Long id);

    /**
     * Search employees by name, department, and status with pagination.
     */
    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN e.department d " +
           "WHERE (:name IS NULL OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE :name) " +
           "AND (:departmentId IS NULL OR d.id = :departmentId) " +
           "AND (:status IS NULL OR e.status = :status)")
    Page<Employee> searchEmployees(
            @Param("name") String name,
            @Param("departmentId") Long departmentId,
            @Param("status") Employee.EmployeeStatus status,
            Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(e.employeeCode, 4) AS int)), 0) FROM Employee e")
    int findMaxEmployeeCodeNumber();
}
