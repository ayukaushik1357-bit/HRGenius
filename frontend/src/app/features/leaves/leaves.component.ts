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
import { LeaveService } from '../../core/services/leave.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService } from '../../core/services/auth.service';
import { LeaveApplication, LeaveBalance } from '../../core/models/leave.model';

@Component({
  selector: 'app-leaves',
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
  templateUrl: './leaves.component.html',
  styleUrls: ['./leaves.component.scss']
})
export class LeavesComponent implements OnInit {
  balance = signal<LeaveBalance | null>(null);
  myLeaves = signal<LeaveApplication[]>([]);
  pendingApprovals = signal<LeaveApplication[]>([]);

  showApplyModal = false;
  applyForm!: FormGroup;

  myColumns = ['leaveType', 'dates', 'days', 'reason', 'status', 'approvedBy'];
  approvalColumns = ['employee', 'leaveType', 'dates', 'days', 'reason', 'actions'];

  get isManagerOrAdmin(): boolean {
    return this.authService.hasAnyRole(['ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_MANAGER']);
  }

  constructor(
    private leaveService: LeaveService,
    private authService: AuthService,
    private notify: NotificationService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadData();
  }

  initForm(): void {
    this.applyForm = this.fb.group({
      leaveType: ['CASUAL', Validators.required],
      startDate: [new Date().toISOString().substring(0, 10), Validators.required],
      endDate: [new Date().toISOString().substring(0, 10), Validators.required],
      numberOfDays: [1, [Validators.required, Validators.min(0.5)]],
      reason: ['', Validators.required]
    });
  }

  loadData(): void {
    this.leaveService.getLeaveBalance().subscribe(b => this.balance.set(b));
    this.leaveService.getMyLeaves().subscribe(l => this.myLeaves.set(l));
    if (this.isManagerOrAdmin) {
      this.leaveService.getPendingApprovals().subscribe(a => this.pendingApprovals.set(a));
    }
  }

  openApplyModal(): void {
    this.applyForm.reset({
      leaveType: 'CASUAL',
      startDate: new Date().toISOString().substring(0, 10),
      endDate: new Date().toISOString().substring(0, 10),
      numberOfDays: 1,
      reason: ''
    });
    this.showApplyModal = true;
  }

  closeApplyModal(): void {
    this.showApplyModal = false;
  }

  submitApplication(): void {
    if (this.applyForm.invalid) return;
    this.leaveService.applyLeave(this.applyForm.value).subscribe({
      next: (created) => {
        this.myLeaves.update(list => [created, ...list]);
        this.notify.success('Leave applied successfully! Pending manager approval.');
        this.leaveService.getLeaveBalance().subscribe(b => this.balance.set(b));
        this.closeApplyModal();
      },
      error: (err) => this.notify.error(err.error?.message || 'Failed to submit leave application')
    });
  }

  actionLeave(app: LeaveApplication, approved: boolean): void {
    const reason = approved ? undefined : prompt('Enter rejection reason:') || 'Not approved';
    this.leaveService.actionLeave(app.id, approved, reason).subscribe({
      next: () => {
        this.pendingApprovals.update(list => list.filter(l => l.id !== app.id));
        this.notify.success(approved ? 'Leave approved!' : 'Leave rejected.');
        this.loadData();
      },
      error: (err) => this.notify.error(err.error?.message || 'Failed to action leave')
    });
  }
}
