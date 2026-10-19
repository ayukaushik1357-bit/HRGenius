package com.hrgenius.repository;

import com.hrgenius.entity.recruitment.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {

    long countByDepartmentId(Long departmentId);

    long countByDesignationId(Long designationId);

    List<JobPosting> findByStatus(JobPosting.JobPostingStatus status);
}
