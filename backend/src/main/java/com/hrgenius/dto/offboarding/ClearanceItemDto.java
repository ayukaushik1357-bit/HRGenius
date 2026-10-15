package com.hrgenius.dto.offboarding;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class ClearanceItemDto {

    @Getter
    @Setter
    public static class Request {
        private String departmentType; // IT, FINANCE, HR, ADMIN
        private String title;
        private String status; // PENDING, CLEARED, WAIVED
        private String remarks;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long exitRequestId;
        private String departmentType;
        private String title;
        private String status;
        private Long clearedById;
        private String clearedByName;
        private LocalDateTime clearedAt;
        private String remarks;
    }
}
