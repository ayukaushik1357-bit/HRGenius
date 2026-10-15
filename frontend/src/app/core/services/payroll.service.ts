import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SalaryStructure, PayrollRun, Payslip } from '../models/payroll.model';

@Injectable({
  providedIn: 'root'
})
export class PayrollService {
  private readonly baseUrl = `${environment.apiUrl}/payroll`;

  constructor(private http: HttpClient) {}

  getSalaryStructure(employeeId: number): Observable<SalaryStructure> {
    return this.http.get<SalaryStructure>(`${this.baseUrl}/salary-structure/${employeeId}`);
  }

  saveSalaryStructure(request: Partial<SalaryStructure>): Observable<SalaryStructure> {
    return this.http.post<SalaryStructure>(`${this.baseUrl}/salary-structure`, request);
  }

  getPayrollRuns(): Observable<PayrollRun[]> {
    return this.http.get<PayrollRun[]>(`${this.baseUrl}/runs`);
  }

  getPayrollRunById(id: number): Observable<PayrollRun> {
    return this.http.get<PayrollRun>(`${this.baseUrl}/runs/${id}`);
  }

  processPayroll(year: number, month: number): Observable<PayrollRun> {
    return this.http.post<PayrollRun>(`${this.baseUrl}/runs`, { year, month });
  }

  markAsPaid(id: number): Observable<PayrollRun> {
    return this.http.post<PayrollRun>(`${this.baseUrl}/runs/${id}/pay`, {});
  }

  getPayslipsByRun(runId: number): Observable<Payslip[]> {
    return this.http.get<Payslip[]>(`${this.baseUrl}/runs/${runId}/payslips`);
  }

  getMyPayslips(): Observable<Payslip[]> {
    return this.http.get<Payslip[]>(`${this.baseUrl}/my-payslips`);
  }

  getEmployeePayslips(employeeId: number): Observable<Payslip[]> {
    return this.http.get<Payslip[]>(`${this.baseUrl}/employee/${employeeId}/payslips`);
  }

  getPayslipById(id: number): Observable<Payslip> {
    return this.http.get<Payslip>(`${this.baseUrl}/payslips/${id}`);
  }

  downloadPayslipPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/payslips/${id}/pdf`, { responseType: 'blob' });
  }
}
