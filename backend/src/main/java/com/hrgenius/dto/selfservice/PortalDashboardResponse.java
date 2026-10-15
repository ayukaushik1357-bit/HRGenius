package com.hrgenius.dto.selfservice;

import com.hrgenius.dto.attendance.AttendanceResponse;
import com.hrgenius.dto.attendance.HolidayResponse;
import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.dto.leave.LeaveBalanceResponse;
import com.hrgenius.dto.payroll.PayslipResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PortalDashboardResponse {
    private EmployeeResponse profile;
    private AttendanceResponse todayAttendance;
    private LeaveBalanceResponse leaveBalance;
    private PayslipResponse latestPayslip;
    private List<HolidayResponse> upcomingHolidays;
    private long pendingTasksCount;
    private long unreadNotificationsCount;
}
