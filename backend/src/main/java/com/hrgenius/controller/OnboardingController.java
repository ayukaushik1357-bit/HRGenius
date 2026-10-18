package com.hrgenius.controller;

import com.hrgenius.dto.onboarding.*;
import com.hrgenius.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "Checklist templates, auto-assignment, task tracking, and progress metrics")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "List onboarding templates")
    public ResponseEntity<List<OnboardingTemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(onboardingService.getAllTemplates());
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get onboarding template by ID")
    public ResponseEntity<OnboardingTemplateResponse> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(onboardingService.getTemplateById(id));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Create onboarding template with default tasks")
    public ResponseEntity<OnboardingTemplateResponse> createTemplate(@Valid @RequestBody OnboardingTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.createTemplate(request));
    }

    @GetMapping("/employees/{employeeId}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or @authz.canViewEmployee(#employeeId)")
    @Operation(summary = "Get onboarding progress and tasks for employee")
    public ResponseEntity<OnboardingProgressResponse> getEmployeeProgress(@PathVariable Long employeeId) {
        return ResponseEntity.ok(onboardingService.getEmployeeProgress(employeeId));
    }

    @PostMapping("/employees/{employeeId}/assign-template/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Assign onboarding checklist template to employee")
    public ResponseEntity<Map<String, String>> assignChecklist(
            @PathVariable Long employeeId,
            @PathVariable Long templateId,
            @RequestParam(required = false) Long ownerId) {
        onboardingService.assignChecklistToEmployee(employeeId, templateId, ownerId);
        return ResponseEntity.ok(Map.of("message", "Onboarding checklist assigned successfully"));
    }

    @PostMapping("/employees/{employeeId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or @authz.isManagerOf(#employeeId)")
    @Operation(summary = "Add ad-hoc onboarding task to employee")
    public ResponseEntity<OnboardingTaskResponse> addTask(
            @PathVariable Long employeeId,
            @Valid @RequestBody OnboardingTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.addTaskToEmployee(employeeId, request));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @Operation(summary = "Update onboarding task status (PENDING, IN_PROGRESS, COMPLETED)")
    public ResponseEntity<OnboardingTaskResponse> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(onboardingService.updateTaskStatus(taskId, body.get("status")));
    }
}
