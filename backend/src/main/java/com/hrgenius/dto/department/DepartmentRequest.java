package com.hrgenius.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Department code is required")
    @Size(max = 20)
    private String code;

    @Size(max = 500)
    private String description;
}
