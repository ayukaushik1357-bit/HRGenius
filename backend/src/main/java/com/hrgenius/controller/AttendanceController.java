package com.hrgenius.controller;

import com.hrgenius.dto.attendance.AttendanceCheckInRequest;
import com.hrgenius.dto.attendance.AttendanceResponse;
import com.hrgenius.dto.attendance.HolidayRequest;
import com.hrgenius.dto.attendance.HolidayResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.security.CurrentUserService;
import com.hrgenius.security.UserPrincipal;
import com.hrgenius.service.AttendanceService;
import com.hrgenius.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance & Leaves", description = "Check-in/out, monthly attendance calendar, and holiday calendar")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final HolidayService holidayService;
    private final CurrentUserService currentUserService;

    @PostMapping("/check-in")
    @Operation(summary = "Check in for current day")
    public ResponseEntity<AttendanceResponse> checkIn(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long employeeId,
            @RequestBody(required = false) AttendanceCheckInRequest request) {
        Long empId = currentUserService.getEmployeeIdForUser(principal, employeeId);
        return ResponseEntity.ok(attendanceService.checkIn(empId, request));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Check out for current day and calculate working hours")
    public ResponseEntity<AttendanceResponse> checkOut(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long employeeId) {
        Long empId = currentUserService.getEmployeeIdForUser(principal, employeeId);
        return ResponseEntity.ok(attendanceService.checkOut(empId));
    }

    @GetMapping("/today")
    @Operation(summary = "Get today's attendance status")
    public ResponseEntity<AttendanceResponse> getTodayAttendance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long employeeId) {
        Long empId = currentUserService.getEmployeeIdForUser(principal, employeeId);
        return ResponseEntity.ok(attendanceService.getTodayAttendance(empId));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly attendance calendar for an employee")
    public ResponseEntity<List<AttendanceResponse>> getMonthlyAttendance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month) {
        Long empId = currentUserService.getEmployeeIdForUser(principal, employeeId);
        return ResponseEntity.ok(attendanceService.getMonthlyAttendance(empId, year, month));
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get daily attendance for all employees")
    public ResponseEntity<List<AttendanceResponse>> getDailyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceByDate(date));
    }

    // --- Holiday Calendar ---
    @GetMapping("/holidays")
    @Operation(summary = "List holidays for given year")
    public ResponseEntity<List<HolidayResponse>> getHolidays(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return ResponseEntity.ok(holidayService.getHolidaysForYear(year));
    }

    @PostMapping("/holidays")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Add holiday to company calendar")
    public ResponseEntity<HolidayResponse> createHoliday(@Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.createHoliday(request));
    }

    @DeleteMapping("/holidays/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Delete holiday from calendar")
    public ResponseEntity<Map<String, String>> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok(Map.of("message", "Holiday removed successfully"));
    }
}
