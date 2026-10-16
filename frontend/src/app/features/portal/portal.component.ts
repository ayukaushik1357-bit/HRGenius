import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SelfServiceService } from '../../core/services/self-service.service';
import { AttendanceService } from '../../core/services/attendance.service';
import { PayrollService } from '../../core/services/payroll.service';
import { AuthService } from '../../core/services/auth.service';
import { PortalDashboard, ProfileUpdateRequest } from '../../core/models/selfservice.model';

@Component({
  selector: 'app-portal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule
  ],
  templateUrl: './portal.component.html',
  styleUrls: ['./portal.component.scss']
})
export class PortalComponent implements OnInit {
  dashboard: PortalDashboard | null = null;
  myRequests: ProfileUpdateRequest[] = [];
  pendingAdminRequests: ProfileUpdateRequest[] = [];
  isAdminOrHr: boolean = false;
  loading: boolean = true;
  hasError: boolean = false;
  errorMessage: string = '';

  // Profile update request modal
  showUpdateModal: boolean = false;
  updateForm = {
    phone: '',
    bankAccountNumber: '',
    bankIfscCode: '',
    bankName: ''
  };

  constructor(
    private selfService: SelfServiceService,
    private attendanceService: AttendanceService,
    private payrollService: PayrollService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const roles = this.authService.currentUser()?.roles || [];
    this.isAdminOrHr = roles.includes('ROLE_ADMIN') || roles.includes('ROLE_HR_MANAGER');

    this.loadDashboard();
    this.loadRequests();
  }

  loadDashboard(): void {
    this.loading = true;
    this.hasError = false;
    this.errorMessage = '';
    this.selfService.getPortalDashboard().subscribe({
      next: (data: PortalDashboard) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.hasError = true;
        this.errorMessage = err.error?.message || err.message || 'Failed to connect to the backend server';
        this.snackBar.open(this.errorMessage, 'Dismiss', { duration: 5000 });
      }
    });
  }

  loadRequests(): void {
    this.selfService.getMyRequests().subscribe({
      next: (reqs: ProfileUpdateRequest[]) => (this.myRequests = reqs)
    });

    if (this.isAdminOrHr) {
      this.selfService.getAllPendingRequests().subscribe({
        next: (reqs: ProfileUpdateRequest[]) => (this.pendingAdminRequests = reqs)
      });
    }
  }

  punchIn(): void {
    this.attendanceService.punchIn().subscribe({
      next: () => {
        this.snackBar.open('Checked in successfully!', 'OK', { duration: 3000 });
        this.loadDashboard();
      },
      error: (err: any) => this.snackBar.open(err.error?.message || 'Check-in error', 'Close', { duration: 3000 })
    });
  }

  punchOut(): void {
    this.attendanceService.punchOut().subscribe({
      next: () => {
        this.snackBar.open('Checked out successfully!', 'OK', { duration: 3000 });
        this.loadDashboard();
      },
      error: (err: any) => this.snackBar.open(err.error?.message || 'Check-out error', 'Close', { duration: 3000 })
    });
  }

  downloadLatestPayslip(): void {
    if (!this.dashboard?.latestPayslip) return;
    this.payrollService.downloadPayslipPdf(this.dashboard.latestPayslip.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Payslip_${this.dashboard?.latestPayslip?.year}_${this.dashboard?.latestPayslip?.month}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
    });
  }

  openUpdateModal(): void {
    if (this.dashboard?.profile) {
      this.updateForm = {
        phone: this.dashboard.profile.phone || '',
        bankAccountNumber: '',
        bankIfscCode: '',
        bankName: ''
      };
    }
    this.showUpdateModal = true;
  }

  submitUpdateRequest(): void {
    const changes: Record<string, any> = {};
    if (this.updateForm.phone) changes['phone'] = this.updateForm.phone;
    if (this.updateForm.bankAccountNumber) changes['bankAccountNumber'] = this.updateForm.bankAccountNumber;
    if (this.updateForm.bankIfscCode) changes['bankIfscCode'] = this.updateForm.bankIfscCode;
    if (this.updateForm.bankName) changes['bankName'] = this.updateForm.bankName;

    this.selfService.submitProfileUpdateRequest(changes).subscribe({
      next: () => {
        this.showUpdateModal = false;
        this.loadRequests();
        this.snackBar.open('Profile change request submitted for HR approval!', 'OK', { duration: 3500 });
      }
    });
  }

  reviewRequest(req: ProfileUpdateRequest, approved: boolean): void {
    this.selfService.reviewProfileUpdate(req.id, approved, approved ? 'Approved by HR' : 'Rejected').subscribe({
      next: () => {
        this.loadRequests();
        this.loadDashboard();
        this.snackBar.open(`Request ${approved ? 'approved and applied' : 'rejected'}!`, 'OK', { duration: 3000 });
      }
    });
  }
}
