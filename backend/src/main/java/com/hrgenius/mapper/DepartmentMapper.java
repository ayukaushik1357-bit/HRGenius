package com.hrgenius.mapper;

import com.hrgenius.dto.department.DepartmentRequest;
import com.hrgenius.dto.department.DepartmentResponse;
import com.hrgenius.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department entity) {
        return DepartmentResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public Department toEntity(DepartmentRequest request) {
        Department dept = new Department();
        dept.setName(request.getName());
        dept.setCode(request.getCode());
        dept.setDescription(request.getDescription());
        return dept;
    }

    public void updateEntity(Department entity, DepartmentRequest request) {
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setDescription(request.getDescription());
    }
}
