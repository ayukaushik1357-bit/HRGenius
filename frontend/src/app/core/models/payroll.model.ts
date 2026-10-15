export interface SalaryStructure {
  id?: number;
  employeeId: number;
  employeeName?: string;
  employeeCode?: string;
  basicSalary: number;
  hra: number;
  specialAllowance: number;
  otherAllowances: number;
  monthlyGross?: number;
  effectiveFrom: string;
}

export interface PayrollRun {
  id: number;
  year: number;
  month: number;
  totalEmployees: number;
  totalGrossPay: number;
  totalNetPay: number;
  status: 'DRAFT' | 'PROCESSED' | 'PAID';
  processedAt?: string;
  paidAt?: string;
}

export interface Payslip {
  id: number;
  payrollRunId: number;
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  departmentName?: string;
  designationTitle?: string;
  year: number;
  month: number;
  totalWorkingDays: number;
  presentDays: number;
  unpaidLeaveDays: number;
  basicSalary: number;
  hra: number;
  specialAllowance: number;
  otherAllowances: number;
  grossSalary: number;
  lopDeduction: number;
  pfDeduction: number;
  esiDeduction: number;
  ptDeduction: number;
  tdsDeduction: number;
  totalDeductions: number;
  netSalary: number;
  status: string;
}
