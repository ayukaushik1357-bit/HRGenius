package com.hrgenius.controller;

import com.hrgenius.dto.analytics.AnalyticsDashboardResponse;
import com.hrgenius.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "HR Analytics", description = "Aggregate metrics, charts, role-based dashboards, Excel/PDF exports")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get aggregated HR analytics dashboard data (company-wide for Admin/HR, team view for Manager)")
    public ResponseEntity<AnalyticsDashboardResponse> getDashboard(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(analyticsService.getDashboardData(role, departmentId));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Export analytics report as Excel workbook (.xlsx)")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        byte[] excelBytes = analyticsService.exportToExcel(year, month);
        String filename = "HRGenius_Analytics_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Export analytics report as PDF document")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        byte[] pdfBytes = analyticsService.exportToPdf(year, month);
        String filename = "HRGenius_Analytics_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
