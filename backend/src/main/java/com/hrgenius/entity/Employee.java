package com.hrgenius.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends BaseEntity {

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum EmploymentType {
        FULL_TIME, PART_TIME, CONTRACT, INTERN
    }

    public enum EmployeeStatus {
        ACTIVE, ON_NOTICE, EXITED
    }

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id")
    private Employee reportingManager;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "salary_ctc", precision = 15, scale = 2)
    private BigDecimal salaryCTC;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "employee", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    // Helper methods for bidirectional relationships
    public void addAddress(Address address) {
        addresses.add(address);
        address.setEmployee(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setEmployee(null);
    }

    public void addEmergencyContact(EmergencyContact contact) {
        emergencyContacts.add(contact);
        contact.setEmployee(this);
    }

    public void removeEmergencyContact(EmergencyContact contact) {
        emergencyContacts.remove(contact);
        contact.setEmployee(null);
    }
}
