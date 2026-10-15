import { Employee } from './employee.model';
import { AttendanceRecord, Holiday } from './attendance.model';
import { LeaveBalance } from './leave.model';
import { Payslip } from './payroll.model';

export interface PortalDashboard {
  profile: Employee;
  todayAttendance?: AttendanceRecord;
  leaveBalance?: LeaveBalance;
  latestPayslip?: Payslip;
  upcomingHolidays: Holiday[];
  pendingTasksCount: number;
  unreadNotificationsCount: number;
}

export interface ProfileUpdateRequest {
  id: number;
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  requestedChanges: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewedById?: number;
  reviewedByName?: string;
  reviewedAt?: string;
  remarks?: string;
  createdAt: string;
}
