package com.hrgenius.controller;

import com.hrgenius.dto.performance.*;
import com.hrgenius.security.CurrentUserService;
import com.hrgenius.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
@Tag(name = "Performance Management", description = "Review cycles, KRAs/Goals, Appraisal workflow, 360 Feedback")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final CurrentUserService currentUserService;

    @GetMapping("/cycles")
    @Operation(summary = "Get all appraisal review cycles")
    public ResponseEntity<List<ReviewCycleResponse>> getAllCycles() {
        return ResponseEntity.ok(performanceService.getAllCycles());
    }

    @PostMapping("/cycles")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Create review cycle and initialize appraisals (Admin/HR)")
    public ResponseEntity<ReviewCycleResponse> createCycle(
            @Valid @RequestBody ReviewCycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.createCycle(request));
    }

    @GetMapping("/goals")
    @Operation(summary = "Get goals for an employee or review cycle")
    public ResponseEntity<List<GoalResponse>> getGoals(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long cycleId) {
        Long targetEmpId = employeeId != null ? employeeId : currentUserService.getCurrentEmployeeOrThrow().getId();
        return ResponseEntity.ok(performanceService.getEmployeeGoals(targetEmpId, cycleId));
    }

    @PostMapping("/goals")
    @Operation(summary = "Create goal or KRA")
    public ResponseEntity<GoalResponse> createGoal(
            @Valid @RequestBody GoalRequest request) {
        if (request.getEmployeeId() == null) {
            request.setEmployeeId(currentUserService.getCurrentEmployeeOrThrow().getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.createGoal(request));
    }

    @PatchMapping("/goals/{goalId}/progress")
    @Operation(summary = "Update progress percentage of goal")
    public ResponseEntity<GoalResponse> updateGoalProgress(
            @PathVariable Long goalId,
            @RequestParam Double progressPercentage) {
        return ResponseEntity.ok(performanceService.updateGoalProgress(goalId, progressPercentage));
    }

    @GetMapping("/reviews/cycle/{cycleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get all appraisals for a review cycle")
    public ResponseEntity<List<PerformanceReviewResponse>> getReviewsForCycle(
            @PathVariable Long cycleId) {
        return ResponseEntity.ok(performanceService.getReviewsForCycle(cycleId));
    }

    @GetMapping("/reviews/team")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get appraisals for manager's direct reports")
    public ResponseEntity<List<PerformanceReviewResponse>> getReviewsForManager() {
        Long empId = currentUserService.getCurrentEmployeeOrThrow().getId();
        return ResponseEntity.ok(performanceService.getReviewsForManager(empId));
    }

    @GetMapping("/reviews/my")
    @Operation(summary = "Get logged-in employee appraisal for a review cycle")
    public ResponseEntity<PerformanceReviewResponse> getMyReview(
            @RequestParam Long cycleId) {
        Long empId = currentUserService.getCurrentEmployeeOrThrow().getId();
        return ResponseEntity.ok(performanceService.getReview(cycleId, empId));
    }

    @PostMapping("/reviews/{reviewId}/self-review")
    @Operation(summary = "Submit self-review rating and comments")
    public ResponseEntity<PerformanceReviewResponse> submitSelfReview(
            @PathVariable Long reviewId,
            @RequestParam Double rating,
            @RequestParam String comments) {
        return ResponseEntity.ok(performanceService.submitSelfReview(reviewId, rating, comments));
    }

    @PostMapping("/reviews/{reviewId}/manager-review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Submit manager review, calculating weighted final score")
    public ResponseEntity<PerformanceReviewResponse> submitManagerReview(
            @PathVariable Long reviewId,
            @RequestParam Double rating,
            @RequestParam String comments) {
        return ResponseEntity.ok(performanceService.submitManagerReview(reviewId, rating, comments));
    }

    @PostMapping("/360-feedback")
    @Operation(summary = "Submit 360 peer feedback")
    public ResponseEntity<PeerFeedbackResponse> submitPeerFeedback(
            @Valid @RequestBody PeerFeedbackRequest request) {
        Long empId = currentUserService.getCurrentEmployeeOrThrow().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.submitPeerFeedback(empId, request));
    }
}
