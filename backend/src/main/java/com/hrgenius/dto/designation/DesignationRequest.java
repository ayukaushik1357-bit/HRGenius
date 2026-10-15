package com.hrgenius.dto.designation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DesignationRequest {

    @NotBlank(message = "Designation title is required")
    @Size(max = 100)
    private String title;

    private Integer level;

    @Size(max = 500)
    private String description;
}
