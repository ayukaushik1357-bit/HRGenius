package com.hrgenius.controller;

import com.hrgenius.dto.offboarding.ClearanceItemDto;
import com.hrgenius.dto.offboarding.ExitRequestDto;
import com.hrgenius.dto.offboarding.FnFSettlementDto;
import com.hrgenius.service.OffboardingService;
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
@RequestMapping("/offboarding")
@RequiredArgsConstructor
@Tag(name = "Compliance & Offboarding", description = "Resignation, Notice period, Clearance checklist, and Full & Final settlement")
public class OffboardingController {

    private final OffboardingService offboardingService;

    @PostMapping("/resign")
    @Operation(summary = "Submit employee resignation")
    public ResponseEntity<ExitRequestDto.Response> submitResignation(
            @Valid @RequestBody ExitRequestDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offboardingService.submitResignation(request));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get exit requests for the logged-in employee")
    public ResponseEntity<List<ExitRequestDto.Response>> getMyExitRequests() {
        return ResponseEntity.ok(offboardingService.getMyExitRequests());
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "List all company exit requests (Admin/HR/Manager)")
    public ResponseEntity<List<ExitRequestDto.Response>> getAllExitRequests() {
        return ResponseEntity.ok(offboardingService.getAllExitRequests());
    }

    @GetMapping("/requests/{id}")
    @Operation(summary = "Get exit request details by id including clearance items and FnF")
    public ResponseEntity<ExitRequestDto.Response> getExitRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(offboardingService.getExitRequestById(id));
    }

    @PostMapping("/requests/{id}/action")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Approve or reject resignation request (Admin/HR/Manager)")
    public ResponseEntity<ExitRequestDto.Response> processExitRequest(
            @PathVariable Long id,
            @RequestBody ExitRequestDto.ActionRequest action) {
        return ResponseEntity.ok(offboardingService.processExitRequest(id, action));
    }

    @PatchMapping("/clearance/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Update status of an offboarding clearance item")
    public ResponseEntity<ClearanceItemDto.Response> updateClearanceItem(
            @PathVariable Long itemId,
            @RequestBody ClearanceItemDto.Request request) {
        return ResponseEntity.ok(offboardingService.updateClearanceItem(itemId, request));
    }

    @PostMapping("/settlement/{settlementId}/finalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Finalize FnF settlement and exit employee (Admin/HR)")
    public ResponseEntity<FnFSettlementDto.Response> finalizeSettlement(
            @PathVariable Long settlementId) {
        return ResponseEntity.ok(offboardingService.finalizeSettlement(settlementId));
    }
}
