package com.hrgenius.dto.performance;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewCycleResponse {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
}
