package com.hrgenius.repository;

import com.hrgenius.entity.recruitment.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    List<Application> findByJobPostingId(Long jobPostingId);
    List<Application> findByStage(Application.ApplicationStage stage);
    Optional<Application> findByJobPostingIdAndCandidateId(Long jobPostingId, Long candidateId);

    @org.springframework.data.jpa.repository.Query("SELECT a.stage, COUNT(a) FROM Application a GROUP BY a.stage")
    List<Object[]> getRecruitmentFunnel();
}

