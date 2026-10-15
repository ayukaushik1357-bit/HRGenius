package com.hrgenius.service;

import com.hrgenius.dto.attendance.AttendanceCheckInRequest;
import com.hrgenius.dto.attendance.AttendanceResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.attendance.Attendance;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.AttendanceMapper;
import com.hrgenius.repository.AttendanceRepository;
import com.hrgenius.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;

    @Transactional
    public AttendanceResponse checkIn(Long employeeId, AttendanceCheckInRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setEmployee(employee);
                    a.setDate(today);
                    return a;
                });

        if (attendance.getCheckInTime() != null) {
            throw new BadRequestException("Employee already checked in today at " + attendance.getCheckInTime());
        }

        attendance.setCheckInTime(now);
        attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
        if (request != null && request.getNotes() != null) {
            attendance.setNotes(request.getNotes());
        }

        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toAttendanceResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new BadRequestException("No check-in record found for today"));

        if (attendance.getCheckInTime() == null) {
            throw new BadRequestException("Cannot check out without checking in first");
        }

        attendance.setCheckOutTime(now);

        long minutes = Duration.between(attendance.getCheckInTime(), now).toMinutes();
        double hours = Math.round((minutes / 60.0) * 100.0) / 100.0;
        attendance.setWorkingHours(hours);

        if (hours >= 7.5) {
            attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
        } else if (hours >= 4.0) {
            attendance.setStatus(Attendance.AttendanceStatus.HALF_DAY);
        } else {
            attendance.setStatus(Attendance.AttendanceStatus.ABSENT);
        }

        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toAttendanceResponse(saved);
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getTodayAttendance(Long employeeId) {
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, LocalDate.now())
                .map(attendanceMapper::toAttendanceResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMonthlyAttendance(Long employeeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end).stream()
                .map(attendanceMapper::toAttendanceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return attendanceRepository.findByDate(target).stream()
                .map(attendanceMapper::toAttendanceResponse)
                .collect(Collectors.toList());
    }
}
