export interface ClearanceItem {
  id: number;
  exitRequestId: number;
  departmentType: 'IT' | 'FINANCE' | 'HR' | 'ADMIN';
  title: string;
  status: 'PENDING' | 'CLEARED' | 'WAIVED';
  clearedById?: number;
  clearedByName?: string;
  clearedAt?: string;
  remarks?: string;
}

export interface FnFSettlement {
  id: number;
  exitRequestId: number;
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  settlementDate: string;
  payableDays: number;
  salaryPayable: number;
  leaveEncashmentDays: number;
  leaveEncashmentAmount: number;
  gratuityAmount: number;
  deductionsAmount: number;
  netPayableAmount: number;
  status: 'DRAFT' | 'PROCESSED' | 'PAID';
  remarks?: string;
}

export interface ExitRequest {
  id: number;
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  departmentName?: string;
  designationTitle?: string;
  resignationDate: string;
  noticePeriodDays: number;
  lastWorkingDay: string;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
  approvedById?: number;
  approvedByName?: string;
  remarks?: string;
  createdAt: string;
  clearanceItems?: ClearanceItem[];
  settlement?: FnFSettlement;
}
