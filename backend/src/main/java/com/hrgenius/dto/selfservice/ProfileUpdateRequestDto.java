package com.hrgenius.dto.selfservice;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class ProfileUpdateRequestDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank(message = "Requested changes cannot be empty")
        private String requestedChanges; // JSON string of proposed changes e.g. phone, address
    }

    @Getter
    @Setter
    public static class ActionRequest {
        private boolean approved;
        private String remarks;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private String employeeCode;
        private String requestedChanges;
        private String status;
        private Long reviewedById;
        private String reviewedByName;
        private LocalDateTime reviewedAt;
        private String remarks;
        private LocalDateTime createdAt;
    }
}
