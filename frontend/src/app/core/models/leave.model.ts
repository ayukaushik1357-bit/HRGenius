export interface LeaveApplication {
  id: number;
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  leaveType: 'CASUAL' | 'SICK' | 'EARNED' | 'UNPAID';
  startDate: string;
  endDate: string;
  numberOfDays: number;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  approvedById?: number;
  approvedByName?: string;
  rejectionReason?: string;
  actionedAt?: string;
  createdAt: string;
}

export interface LeaveBalance {
  id: number;
  employeeId: number;
  employeeName: string;
  year: number;
  casualQuota: number;
  casualUsed: number;
  casualRemaining: number;
  sickQuota: number;
  sickUsed: number;
  sickRemaining: number;
  earnedQuota: number;
  earnedUsed: number;
  earnedRemaining: number;
}

export interface LeaveApplicationRequest {
  leaveType: string;
  startDate: string;
  endDate: string;
  numberOfDays: number;
  reason: string;
}
