import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { PayrollService } from '../../core/services/payroll.service';
import { EmployeeService } from '../../core/services/employee.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService } from '../../core/services/auth.service';
import { PayrollRun, Payslip, SalaryStructure } from '../../core/models/payroll.model';
import { Employee } from '../../core/models/employee.model';

@Component({
  selector: 'app-payroll',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './payroll.component.html',
  styleUrls: ['./payroll.component.scss']
})
export class PayrollComponent implements OnInit {
  payrollRuns = signal<PayrollRun[]>([]);
  myPayslips = signal<Payslip[]>([]);
  runPayslips = signal<Payslip[]>([]);
  employees = signal<Employee[]>([]);

  selectedRun = signal<PayrollRun | null>(null);

  runYear = new Date().getFullYear();
  runMonth = new Date().getMonth() + 1;

  showSalaryModal = false;
  salaryForm!: FormGroup;

  payslipColumns = ['monthYear', 'gross', 'lop', 'pf', 'esi', 'pt', 'tds', 'net', 'status', 'actions'];
  runColumns = ['period', 'employees', 'totalGross', 'totalNet', 'status', 'actions'];

  get isAdminOrHr(): boolean {
    return this.authService.hasAnyRole(['ROLE_ADMIN', 'ROLE_HR_MANAGER']);
  }

  constructor(
    private payrollService: PayrollService,
    private employeeService: EmployeeService,
    private authService: AuthService,
    private notify: NotificationService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initSalaryForm();
    this.loadData();
  }

  initSalaryForm(): void {
    this.salaryForm = this.fb.group({
      employeeId: [null, Validators.required],
      basicSalary: [50000, [Validators.required, Validators.min(1)]],
      hra: [25000, [Validators.required, Validators.min(0)]],
      specialAllowance: [15000],
      otherAllowances: [5000],
      effectiveFrom: [new Date().toISOString().substring(0, 10), Validators.required]
    });
  }

  loadData(): void {
    this.payrollService.getMyPayslips().subscribe(p => this.myPayslips.set(p));

    if (this.isAdminOrHr) {
      this.payrollService.getPayrollRuns().subscribe(runs => {
        this.payrollRuns.set(runs);
        if (runs.length > 0) {
          this.viewRunPayslips(runs[0]);
        }
      });
      this.employeeService.searchEmployees({ size: 100 }).subscribe(e => this.employees.set(e.content));
    }
  }

  processPayrollRun(): void {
    this.payrollService.processPayroll(this.runYear, this.runMonth).subscribe({
      next: (run) => {
        this.notify.success(`Payroll processed for ${this.runMonth}/${this.runYear}!`);
        this.loadData();
        this.viewRunPayslips(run);
      },
      error: (err) => this.notify.error(err.error?.message || 'Failed to process payroll')
    });
  }

  markRunAsPaid(run: PayrollRun): void {
    this.payrollService.markAsPaid(run.id).subscribe({
      next: (updated) => {
        this.notify.success('Payroll locked and marked as PAID! Dispatched notifications.');
        this.loadData();
      },
      error: (err) => this.notify.error(err.error?.message || 'Failed to mark as paid')
    });
  }

  viewRunPayslips(run: PayrollRun): void {
    this.selectedRun.set(run);
    this.payrollService.getPayslipsByRun(run.id).subscribe(p => {
      this.runPayslips.set(p);
    });
  }

  downloadPdf(payslip: Payslip): void {
    this.payrollService.downloadPayslipPdf(payslip.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `payslip_${payslip.employeeCode}_${payslip.year}_${payslip.month}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.notify.success('Payslip PDF downloaded');
      },
      error: () => this.notify.error('Failed to download payslip PDF')
    });
  }

  openSalaryModal(empId?: number): void {
    if (empId) {
      this.payrollService.getSalaryStructure(empId).subscribe({
        next: (ss) => {
          this.salaryForm.patchValue(ss);
          this.showSalaryModal = true;
        },
        error: () => {
          this.salaryForm.reset({
            employeeId: empId,
            basicSalary: 50000,
            hra: 25000,
            specialAllowance: 15000,
            otherAllowances: 5000,
            effectiveFrom: new Date().toISOString().substring(0, 10)
          });
          this.showSalaryModal = true;
        }
      });
    } else {
      this.salaryForm.reset({
        basicSalary: 50000,
        hra: 25000,
        specialAllowance: 15000,
        otherAllowances: 5000,
        effectiveFrom: new Date().toISOString().substring(0, 10)
      });
      this.showSalaryModal = true;
    }
  }

  closeSalaryModal(): void {
    this.showSalaryModal = false;
  }

  saveSalary(): void {
    if (this.salaryForm.invalid) return;
    this.payrollService.saveSalaryStructure(this.salaryForm.value).subscribe({
      next: () => {
        this.notify.success('Salary structure saved successfully!');
        this.closeSalaryModal();
      },
      error: (err) => this.notify.error(err.error?.message || 'Failed to save salary structure')
    });
  }
}
