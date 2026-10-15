package com.hrgenius.dto.employee;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class EmployeeResponse {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate joiningDate;

    // Flattened department / designation
    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationTitle;

    // Reporting manager
    private Long reportingManagerId;
    private String reportingManagerName;

    private String employmentType;
    private String status;
    private BigDecimal salaryCTC;

    private List<AddressDto> addresses;
    private List<EmergencyContactDto> emergencyContacts;
    private List<DocumentDto> documents;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class AddressDto {
        private Long id;
        private String addressType;
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String country;
        private String zipCode;
    }

    @Getter
    @Builder
    public static class EmergencyContactDto {
        private Long id;
        private String name;
        private String relationship;
        private String phone;
    }

    @Getter
    @Builder
    public static class DocumentDto {
        private Long id;
        private String originalFileName;
        private String contentType;
        private Long fileSize;
        private String documentType;
        private LocalDateTime uploadedAt;
    }
}
