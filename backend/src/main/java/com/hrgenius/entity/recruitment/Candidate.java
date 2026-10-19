package com.hrgenius.entity.recruitment;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.hrgenius.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
public class Candidate extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "resume_file_name")
    private String resumeFileName;

    @Column(name = "resume_file_path", length = 500)
    private String resumeFilePath;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "experience_years", precision = 5, scale = 2)
    private Double experienceYears;

    @Column(name = "current_company", length = 100)
    private String currentCompany;

    @Column(name = "expected_salary_ctc", precision = 15, scale = 2)
    private BigDecimal expectedSalaryCTC;
}
