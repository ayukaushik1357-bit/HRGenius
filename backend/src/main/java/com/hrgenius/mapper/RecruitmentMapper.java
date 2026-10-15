package com.hrgenius.mapper;

import com.hrgenius.dto.recruitment.*;
import com.hrgenius.entity.Department;
import com.hrgenius.entity.Designation;
import com.hrgenius.entity.recruitment.*;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentMapper {

    public JobPostingResponse toJobPostingResponse(JobPosting entity) {
        if (entity == null) return null;
        return JobPostingResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .requirements(entity.getRequirements())
                .location(entity.getLocation())
                .minExperienceYears(entity.getMinExperienceYears())
                .maxExperienceYears(entity.getMaxExperienceYears())
                .openings(entity.getOpenings())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .departmentName(entity.getDepartment() != null ? entity.getDepartment().getName() : null)
                .designationId(entity.getDesignation() != null ? entity.getDesignation().getId() : null)
                .designationTitle(entity.getDesignation() != null ? entity.getDesignation().getTitle() : null)
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public JobPosting toJobPostingEntity(JobPostingRequest request, Department department, Designation designation) {
        JobPosting posting = new JobPosting();
        posting.setTitle(request.getTitle());
        posting.setDescription(request.getDescription());
        posting.setRequirements(request.getRequirements());
        posting.setLocation(request.getLocation());
        posting.setMinExperienceYears(request.getMinExperienceYears());
        posting.setMaxExperienceYears(request.getMaxExperienceYears());
        posting.setOpenings(request.getOpenings());
        if (request.getStatus() != null) {
            posting.setStatus(request.getStatus());
        }
        posting.setDepartment(department);
        posting.setDesignation(designation);
        return posting;
    }

    public CandidateResponse toCandidateResponse(Candidate entity) {
        if (entity == null) return null;
        return CandidateResponse.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .resumeFileName(entity.getResumeFileName())
                .resumeFilePath(entity.getResumeFilePath())
                .experienceYears(entity.getExperienceYears())
                .currentCompany(entity.getCurrentCompany())
                .expectedSalaryCTC(entity.getExpectedSalaryCTC())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public Candidate toCandidateEntity(CandidateRequest request) {
        Candidate candidate = new Candidate();
        candidate.setFirstName(request.getFirstName());
        candidate.setLastName(request.getLastName());
        candidate.setEmail(request.getEmail());
        candidate.setPhone(request.getPhone());
        candidate.setExperienceYears(request.getExperienceYears());
        candidate.setCurrentCompany(request.getCurrentCompany());
        candidate.setExpectedSalaryCTC(request.getExpectedSalaryCTC());
        return candidate;
    }

    public ApplicationResponse toApplicationResponse(Application entity) {
        if (entity == null) return null;
        String candidateName = null;
        String candidateEmail = null;
        String candidatePhone = null;
        String resumeFileName = null;
        Long candidateId = null;
        if (entity.getCandidate() != null) {
            candidateId = entity.getCandidate().getId();
            candidateName = entity.getCandidate().getFirstName() + " " + entity.getCandidate().getLastName();
            candidateEmail = entity.getCandidate().getEmail();
            candidatePhone = entity.getCandidate().getPhone();
            resumeFileName = entity.getCandidate().getResumeFileName();
        }

        Long jobPostingId = null;
        String jobPostingTitle = null;
        if (entity.getJobPosting() != null) {
            jobPostingId = entity.getJobPosting().getId();
            jobPostingTitle = entity.getJobPosting().getTitle();
        }

        return ApplicationResponse.builder()
                .id(entity.getId())
                .jobPostingId(jobPostingId)
                .jobPostingTitle(jobPostingTitle)
                .candidateId(candidateId)
                .candidateName(candidateName)
                .candidateEmail(candidateEmail)
                .candidatePhone(candidatePhone)
                .resumeFileName(resumeFileName)
                .stage(entity.getStage() != null ? entity.getStage().name() : null)
                .appliedAt(entity.getAppliedAt())
                .notes(entity.getNotes())
                .build();
    }

    public InterviewResponse toInterviewResponse(Interview entity) {
        if (entity == null) return null;
        String candidateName = null;
        String jobPostingTitle = null;
        Long candidateId = null;
        if (entity.getApplication() != null) {
            if (entity.getApplication().getCandidate() != null) {
                candidateId = entity.getApplication().getCandidate().getId();
                candidateName = entity.getApplication().getCandidate().getFirstName() + " " + entity.getApplication().getCandidate().getLastName();
            }
            if (entity.getApplication().getJobPosting() != null) {
                jobPostingTitle = entity.getApplication().getJobPosting().getTitle();
            }
        }

        String interviewerName = null;
        Long interviewerId = null;
        if (entity.getInterviewer() != null) {
            interviewerId = entity.getInterviewer().getId();
            interviewerName = entity.getInterviewer().getFirstName() + " " + entity.getInterviewer().getLastName();
        }

        return InterviewResponse.builder()
                .id(entity.getId())
                .applicationId(entity.getApplication() != null ? entity.getApplication().getId() : null)
                .candidateId(candidateId)
                .candidateName(candidateName)
                .jobPostingTitle(jobPostingTitle)
                .interviewerId(interviewerId)
                .interviewerName(interviewerName)
                .scheduledAt(entity.getScheduledAt())
                .roundName(entity.getRoundName())
                .rating(entity.getRating())
                .feedback(entity.getFeedback())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .build();
    }

    public OfferResponse toOfferResponse(Offer entity) {
        if (entity == null) return null;
        String candidateName = null;
        String jobPostingTitle = null;
        Long candidateId = null;
        if (entity.getApplication() != null) {
            if (entity.getApplication().getCandidate() != null) {
                candidateId = entity.getApplication().getCandidate().getId();
                candidateName = entity.getApplication().getCandidate().getFirstName() + " " + entity.getApplication().getCandidate().getLastName();
            }
            if (entity.getApplication().getJobPosting() != null) {
                jobPostingTitle = entity.getApplication().getJobPosting().getTitle();
            }
        }

        return OfferResponse.builder()
                .id(entity.getId())
                .applicationId(entity.getApplication() != null ? entity.getApplication().getId() : null)
                .candidateId(candidateId)
                .candidateName(candidateName)
                .jobPostingTitle(jobPostingTitle)
                .offerDate(entity.getOfferDate())
                .joiningDate(entity.getJoiningDate())
                .offeredSalaryCTC(entity.getOfferedSalaryCTC())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .remarks(entity.getRemarks())
                .build();
    }
}
