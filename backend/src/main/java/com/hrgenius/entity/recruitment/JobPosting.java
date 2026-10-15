package com.hrgenius.entity.recruitment;

import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Department;
import com.hrgenius.entity.Designation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_postings")
@Getter
@Setter
@NoArgsConstructor
public class JobPosting extends BaseEntity {

    public enum JobPostingStatus {
        DRAFT, OPEN, CLOSED
    }

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 2000)
    private String requirements;

    @Column(length = 100)
    private String location;

    @Column(name = "min_exp_years")
    private Integer minExperienceYears;

    @Column(name = "max_exp_years")
    private Integer maxExperienceYears;

    @Column(nullable = false)
    private Integer openings = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status = JobPostingStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;
}
