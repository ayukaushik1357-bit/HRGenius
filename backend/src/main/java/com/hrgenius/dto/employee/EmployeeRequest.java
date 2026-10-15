package com.hrgenius.dto.employee;

import com.hrgenius.entity.Employee;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    private LocalDate dateOfBirth;

    private Employee.Gender gender;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private Long departmentId;
    private Long designationId;
    private Long reportingManagerId;

    private Employee.EmploymentType employmentType;
    private Employee.EmployeeStatus status;
    private BigDecimal salaryCTC;

    @Size(max = 20)
    private String panNumber;

    @Size(max = 20)
    private String aadhaarNumber;

    @Size(max = 30)
    private String bankAccountNumber;

    @Size(max = 20)
    private String bankIfscCode;

    @Size(max = 100)
    private String bankName;

    @Valid
    private List<AddressDto> addresses;

    @Valid
    private List<EmergencyContactDto> emergencyContacts;

    @Getter
    @Setter
    public static class AddressDto {
        @NotBlank(message = "Address type is required")
        private String addressType;
        @NotBlank(message = "Address line 1 is required")
        private String line1;
        private String line2;
        @NotBlank(message = "City is required")
        private String city;
        @NotBlank(message = "State is required")
        private String state;
        @NotBlank(message = "Country is required")
        private String country;
        @NotBlank(message = "Zip code is required")
        private String zipCode;
    }

    @Getter
    @Setter
    public static class EmergencyContactDto {
        @NotBlank(message = "Contact name is required")
        private String name;
        @NotBlank(message = "Relationship is required")
        private String relationship;
        @NotBlank(message = "Phone is required")
        private String phone;
    }
}
