package com.hrgenius.controller;

import com.hrgenius.dto.selfservice.PortalDashboardResponse;
import com.hrgenius.dto.selfservice.ProfileUpdateRequestDto;
import com.hrgenius.service.SelfServicePortalService;
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
@RequestMapping("/portal")
@RequiredArgsConstructor
@Tag(name = "Employee Self-Service Portal", description = "Personal portal home, profile update workflow with HR approval")
public class SelfServiceController {

    private final SelfServicePortalService selfServicePortalService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get employee self-service portal dashboard")
    public ResponseEntity<PortalDashboardResponse> getPortalDashboard() {
        return ResponseEntity.ok(selfServicePortalService.getPortalDashboard());
    }

    @PostMapping("/profile-update")
    @Operation(summary = "Submit profile update request for HR approval")
    public ResponseEntity<ProfileUpdateRequestDto.Response> requestProfileUpdate(
            @Valid @RequestBody ProfileUpdateRequestDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(selfServicePortalService.submitProfileUpdateRequest(request));
    }

    @GetMapping("/profile-updates/my")
    @Operation(summary = "Get logged-in employee profile update requests")
    public ResponseEntity<List<ProfileUpdateRequestDto.Response>> getMyRequests() {
        return ResponseEntity.ok(selfServicePortalService.getMyRequests());
    }

    @GetMapping("/profile-updates")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get all pending employee profile update requests (Admin/HR)")
    public ResponseEntity<List<ProfileUpdateRequestDto.Response>> getAllPendingRequests() {
        return ResponseEntity.ok(selfServicePortalService.getAllPendingRequests());
    }

    @PostMapping("/profile-updates/{id}/action")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Approve or reject employee profile update request (Admin/HR)")
    public ResponseEntity<ProfileUpdateRequestDto.Response> reviewProfileUpdateRequest(
            @PathVariable Long id,
            @RequestBody ProfileUpdateRequestDto.ActionRequest action) {
        return ResponseEntity.ok(selfServicePortalService.reviewProfileUpdateRequest(id, action));
    }
}
