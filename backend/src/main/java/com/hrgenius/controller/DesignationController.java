package com.hrgenius.controller;

import com.hrgenius.dto.designation.DesignationRequest;
import com.hrgenius.dto.designation.DesignationResponse;
import com.hrgenius.service.DesignationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/designations")
@RequiredArgsConstructor
@Tag(name = "Designations", description = "Designation and job title management APIs")
public class DesignationController {

    private final DesignationService designationService;

    @GetMapping
    @Operation(summary = "Get all designations")
    public ResponseEntity<List<DesignationResponse>> getAllDesignations() {
        return ResponseEntity.ok(designationService.getAllDesignations());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get designation by ID")
    public ResponseEntity<DesignationResponse> getDesignationById(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.getDesignationById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Create designation (ADMIN or HR_MANAGER)")
    public ResponseEntity<DesignationResponse> createDesignation(@Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(designationService.createDesignation(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Update designation (ADMIN or HR_MANAGER)")
    public ResponseEntity<DesignationResponse> updateDesignation(
            @PathVariable Long id,
            @Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.ok(designationService.updateDesignation(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete designation (ADMIN only)")
    public ResponseEntity<Map<String, String>> deleteDesignation(@PathVariable Long id) {
        designationService.deleteDesignation(id);
        return ResponseEntity.ok(Map.of("message", "Designation deleted successfully"));
    }
}
