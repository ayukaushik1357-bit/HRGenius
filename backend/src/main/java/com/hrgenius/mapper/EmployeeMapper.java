package com.hrgenius.mapper;

import com.hrgenius.dto.employee.EmployeeRequest;
import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.entity.Address;
import com.hrgenius.entity.Document;
import com.hrgenius.entity.EmergencyContact;
import com.hrgenius.entity.Employee;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee e) {
        boolean privileged = isHrOrAdmin();
        return EmployeeResponse.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .dateOfBirth(e.getDateOfBirth())
                .gender(e.getGender() != null ? e.getGender().name() : null)
                .joiningDate(e.getJoiningDate())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .designationId(e.getDesignation() != null ? e.getDesignation().getId() : null)
                .designationTitle(e.getDesignation() != null ? e.getDesignation().getTitle() : null)
                .reportingManagerId(e.getReportingManager() != null ? e.getReportingManager().getId() : null)
                .reportingManagerName(e.getReportingManager() != null
                        ? e.getReportingManager().getFirstName() + " " + e.getReportingManager().getLastName()
                        : null)
                .employmentType(e.getEmploymentType() != null ? e.getEmploymentType().name() : null)
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .salaryCTC(privileged ? e.getSalaryCTC() : null)
                .panNumber(privileged ? e.getPanNumber() : maskPan(e.getPanNumber()))
                .aadhaarNumber(privileged ? e.getAadhaarNumber() : maskAadhaar(e.getAadhaarNumber()))
                .bankAccountNumber(privileged ? e.getBankAccountNumber() : maskBankAccount(e.getBankAccountNumber()))
                .bankIfscCode(privileged ? e.getBankIfscCode() : (e.getBankIfscCode() != null ? "XXXX" : null))
                .bankName(e.getBankName())
                .addresses(mapAddresses(e.getAddresses()))
                .emergencyContacts(mapEmergencyContacts(e.getEmergencyContacts()))
                .documents(mapDocuments(e.getDocuments()))
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .updatedBy(e.getUpdatedBy())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    /**
     * Lightweight response for list views (no nested collections).
     */
    public EmployeeResponse toListResponse(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .gender(e.getGender() != null ? e.getGender().name() : null)
                .joiningDate(e.getJoiningDate())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .designationId(e.getDesignation() != null ? e.getDesignation().getId() : null)
                .designationTitle(e.getDesignation() != null ? e.getDesignation().getTitle() : null)
                .employmentType(e.getEmploymentType() != null ? e.getEmploymentType().name() : null)
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .build();
    }

    public Employee toEntity(EmployeeRequest req) {
        Employee emp = new Employee();
        applyFields(emp, req);
        return emp;
    }

    public void updateEntity(Employee emp, EmployeeRequest req) {
        applyFields(emp, req);
    }

    // ── private helpers ──────────────────────────────────────

    private void applyFields(Employee emp, EmployeeRequest req) {
        emp.setFirstName(req.getFirstName());
        emp.setLastName(req.getLastName());
        emp.setEmail(req.getEmail());
        emp.setPhone(req.getPhone());
        emp.setDateOfBirth(req.getDateOfBirth());
        emp.setGender(req.getGender());
        emp.setJoiningDate(req.getJoiningDate());
        emp.setEmploymentType(req.getEmploymentType() != null
                ? req.getEmploymentType() : Employee.EmploymentType.FULL_TIME);
        emp.setStatus(req.getStatus() != null
                ? req.getStatus() : Employee.EmployeeStatus.ACTIVE);
        emp.setSalaryCTC(req.getSalaryCTC());
        emp.setPanNumber(req.getPanNumber());
        emp.setAadhaarNumber(req.getAadhaarNumber());
        emp.setBankAccountNumber(req.getBankAccountNumber());
        emp.setBankIfscCode(req.getBankIfscCode());
        emp.setBankName(req.getBankName());
    }

    private boolean isHrOrAdmin() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> 
            a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR_MANAGER"));
    }

    private String maskAadhaar(String val) {
        if (val == null || val.isBlank()) return val;
        String clean = val.replaceAll("[^0-9]", "");
        if (clean.length() >= 4) {
            return "XXXX-XXXX-" + clean.substring(clean.length() - 4);
        }
        return "XXXX-XXXX-XXXX";
    }

    private String maskPan(String val) {
        if (val == null || val.isBlank()) return val;
        if (val.length() >= 4) {
            return "XXXXXX" + val.substring(val.length() - 4);
        }
        return "XXXXXXXXXX";
    }

    private String maskBankAccount(String val) {
        if (val == null || val.isBlank()) return val;
        if (val.length() >= 4) {
            return "XXXXXXXX" + val.substring(val.length() - 4);
        }
        return "XXXXXXXXXXXX";
    }

    private List<EmployeeResponse.AddressDto> mapAddresses(List<Address> addresses) {
        if (addresses == null) return Collections.emptyList();
        return addresses.stream()
                .map(a -> EmployeeResponse.AddressDto.builder()
                        .id(a.getId())
                        .addressType(a.getAddressType() != null ? a.getAddressType().name() : null)
                        .line1(a.getLine1())
                        .line2(a.getLine2())
                        .city(a.getCity())
                        .state(a.getState())
                        .country(a.getCountry())
                        .zipCode(a.getZipCode())
                        .build())
                .toList();
    }

    private List<EmployeeResponse.EmergencyContactDto> mapEmergencyContacts(
            List<EmergencyContact> contacts) {
        if (contacts == null) return Collections.emptyList();
        return contacts.stream()
                .map(c -> EmployeeResponse.EmergencyContactDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .relationship(c.getRelationship())
                        .phone(c.getPhone())
                        .build())
                .toList();
    }

    private List<EmployeeResponse.DocumentDto> mapDocuments(List<Document> documents) {
        if (documents == null) return Collections.emptyList();
        return documents.stream()
                .map(d -> EmployeeResponse.DocumentDto.builder()
                        .id(d.getId())
                        .originalFileName(d.getOriginalFileName())
                        .contentType(d.getContentType())
                        .fileSize(d.getFileSize())
                        .documentType(d.getDocumentType() != null ? d.getDocumentType().name() : null)
                        .uploadedAt(d.getCreatedAt())
                        .build())
                .toList();
    }
}
