export interface AttendanceRecord {
  id: number;
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  date: string;
  checkInTime?: string;
  checkOutTime?: string;
  workingHours?: number;
  status: 'PRESENT' | 'HALF_DAY' | 'ABSENT';
  notes?: string;
}

export interface Holiday {
  id: number;
  holidayDate: string;
  name: string;
  description?: string;
  optional: boolean;
}
