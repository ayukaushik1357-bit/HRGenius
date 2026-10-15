package com.hrgenius.repository;

import com.hrgenius.entity.recruitment.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByApplicationId(Long applicationId);
    List<Interview> findByInterviewerId(Long interviewerId);
}
