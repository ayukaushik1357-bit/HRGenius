package com.hrgenius.dto.department;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
