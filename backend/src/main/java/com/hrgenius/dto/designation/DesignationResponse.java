package com.hrgenius.dto.designation;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DesignationResponse {
    private Long id;
    private String title;
    private Integer level;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
