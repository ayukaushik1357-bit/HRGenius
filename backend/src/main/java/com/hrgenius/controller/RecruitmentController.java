package com.hrgenius.controller;

import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.dto.recruitment.*;
import com.hrgenius.entity.recruitment.Application;
import com.hrgenius.entity.recruitment.JobPosting;
import com.hrgenius.service.RecruitmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recruitment")
@RequiredArgsConstructor
@Tag(name = "Recruitment", description = "Job postings, candidates, interviews, offers, and hiring pipeline")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    // --- Job Postings ---
    @GetMapping("/jobs")
    @Operation(summary = "List all job postings")
    public ResponseEntity<List<JobPostingResponse>> getAllJobPostings(
            @RequestParam(required = false) JobPosting.JobPostingStatus status) {
        return ResponseEntity.ok(recruitmentService.getAllJobPostings(status));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get job posting by ID")
    public ResponseEntity<JobPostingResponse> getJobPostingById(@PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getJobPostingById(id));
    }

    @PostMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Create job posting")
    public ResponseEntity<JobPostingResponse> createJobPosting(@Valid @RequestBody JobPostingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recruitmentService.createJobPosting(request));
    }

    @PutMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Update job posting")
    public ResponseEntity<JobPostingResponse> updateJobPosting(@PathVariable Long id, @Valid @RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(recruitmentService.updateJobPosting(id, request));
    }

    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Delete job posting")
    public ResponseEntity<Map<String, String>> deleteJobPosting(@PathVariable Long id) {
        recruitmentService.deleteJobPosting(id);
        return ResponseEntity.ok(Map.of("message", "Job posting deleted successfully"));
    }

    // --- Candidates ---
    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "List all candidates")
    public ResponseEntity<List<CandidateResponse>> getAllCandidates() {
        return ResponseEntity.ok(recruitmentService.getAllCandidates());
    }

    @GetMapping("/candidates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get candidate by ID")
    public ResponseEntity<CandidateResponse> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getCandidateById(id));
    }

    @PostMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Create candidate record")
    public ResponseEntity<CandidateResponse> createCandidate(@Valid @RequestBody CandidateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recruitmentService.createCandidate(request));
    }

    @PostMapping(value = "/candidates/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Upload candidate resume")
    public ResponseEntity<CandidateResponse> uploadResume(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(recruitmentService.uploadResume(id, file));
    }

    @GetMapping("/candidates/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Download candidate resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id) throws IOException {
        CandidateResponse c = recruitmentService.getCandidateById(id);
        Resource resource = recruitmentService.downloadResume(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + c.getResumeFileName() + "\"")
                .body(resource);
    }

    // --- Applications ---
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "List applications (filter by job posting or stage)")
    public ResponseEntity<List<ApplicationResponse>> getApplications(
            @RequestParam(required = false) Long jobPostingId,
            @RequestParam(required = false) Application.ApplicationStage stage) {
        return ResponseEntity.ok(recruitmentService.getApplications(jobPostingId, stage));
    }

    @PostMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Submit application for candidate")
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody ApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recruitmentService.createApplication(request));
    }

    @PatchMapping("/applications/{id}/stage")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Update application stage (Kanban move)")
    public ResponseEntity<ApplicationResponse> updateStage(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(recruitmentService.updateApplicationStage(id, body.get("stage")));
    }

    // --- Interviews ---
    @GetMapping("/interviews/application/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "List interviews for application")
    public ResponseEntity<List<InterviewResponse>> getInterviewsByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(recruitmentService.getInterviewsByApplication(applicationId));
    }

    @PostMapping("/interviews")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Schedule interview")
    public ResponseEntity<InterviewResponse> scheduleInterview(@Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recruitmentService.scheduleInterview(request));
    }

    @PutMapping("/interviews/{id}/feedback")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Submit interview feedback and rating")
    public ResponseEntity<InterviewResponse> submitFeedback(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer rating = body.get("rating") != null ? Integer.valueOf(body.get("rating").toString()) : null;
        String feedback = (String) body.get("feedback");
        String status = (String) body.get("status");
        return ResponseEntity.ok(recruitmentService.submitInterviewFeedback(id, rating, feedback, status));
    }

    // --- Offers ---
    @GetMapping("/offers/application/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get offer for application")
    public ResponseEntity<OfferResponse> getOfferByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(recruitmentService.getOfferByApplication(applicationId));
    }

    @PostMapping("/offers")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Create or update job offer")
    public ResponseEntity<OfferResponse> createOrUpdateOffer(@Valid @RequestBody OfferRequest request) {
        return ResponseEntity.ok(recruitmentService.createOrUpdateOffer(request));
    }

    // --- Hire to Employee ---
    @PostMapping("/hire")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Convert hired candidate into an employee with onboarding checklist")
    public ResponseEntity<EmployeeResponse> hireCandidate(@Valid @RequestBody HireCandidateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recruitmentService.hireCandidate(request));
    }
}
