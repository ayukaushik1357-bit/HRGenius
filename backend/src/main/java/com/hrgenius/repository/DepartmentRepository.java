package com.hrgenius.repository;

import com.hrgenius.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);
    Optional<Department> findByName(String name);
    boolean existsByName(String name);
    boolean existsByCode(String code);
}
