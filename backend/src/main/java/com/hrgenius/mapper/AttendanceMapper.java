package com.hrgenius.mapper;

import com.hrgenius.dto.attendance.AttendanceResponse;
import com.hrgenius.dto.attendance.HolidayRequest;
import com.hrgenius.dto.attendance.HolidayResponse;
import com.hrgenius.entity.attendance.Attendance;
import com.hrgenius.entity.attendance.Holiday;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponse toAttendanceResponse(Attendance entity) {
        if (entity == null) return null;
        String employeeName = null;
        String employeeCode = null;
        Long employeeId = null;
        if (entity.getEmployee() != null) {
            employeeId = entity.getEmployee().getId();
            employeeName = entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName();
            employeeCode = entity.getEmployee().getEmployeeCode();
        }

        return AttendanceResponse.builder()
                .id(entity.getId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .date(entity.getDate())
                .checkInTime(entity.getCheckInTime())
                .checkOutTime(entity.getCheckOutTime())
                .workingHours(entity.getWorkingHours())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .notes(entity.getNotes())
                .build();
    }

    public HolidayResponse toHolidayResponse(Holiday entity) {
        if (entity == null) return null;
        return HolidayResponse.builder()
                .id(entity.getId())
                .holidayDate(entity.getHolidayDate())
                .name(entity.getName())
                .description(entity.getDescription())
                .optional(entity.isOptional())
                .build();
    }

    public Holiday toHolidayEntity(HolidayRequest request) {
        Holiday h = new Holiday();
        h.setHolidayDate(request.getHolidayDate());
        h.setName(request.getName());
        h.setDescription(request.getDescription());
        h.setOptional(request.isOptional());
        return h;
    }
}
