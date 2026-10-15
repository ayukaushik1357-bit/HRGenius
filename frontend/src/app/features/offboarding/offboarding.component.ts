import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OffboardingService } from '../../core/services/offboarding.service';
import { AuthService } from '../../core/services/auth.service';
import { ExitRequest, ClearanceItem, FnFSettlement } from '../../core/models/offboarding.model';

@Component({
  selector: 'app-offboarding',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatTableModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl: './offboarding.component.html',
  styleUrls: ['./offboarding.component.scss']
})
export class OffboardingComponent implements OnInit {
  myRequests: ExitRequest[] = [];
  allRequests: ExitRequest[] = [];
  selectedRequest: ExitRequest | null = null;

  isAdminOrHr: boolean = false;
  loading: boolean = true;

  // New Resignation form
  showResignationModal: boolean = false;
  resignationForm = {
    resignationDate: new Date().toISOString().slice(0, 10),
    noticePeriodDays: 30,
    lastWorkingDay: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10),
    reason: ''
  };

  // Review Resignation Modal
  reviewAction = { approved: true, remarks: '', agreedLastWorkingDay: '' };
  showReviewModal: boolean = false;

  constructor(
    private offboardingService: OffboardingService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const roles = this.authService.currentUser()?.roles || [];
    this.isAdminOrHr = roles.includes('ROLE_ADMIN') || roles.includes('ROLE_HR_MANAGER');

    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.offboardingService.getMyExitRequests().subscribe({
      next: (reqs: ExitRequest[]) => {
        this.myRequests = reqs;
        if (reqs.length > 0 && !this.selectedRequest) {
          this.viewDetails(reqs[0]);
        }
      }
    });

    if (this.isAdminOrHr) {
      this.offboardingService.getAllExitRequests().subscribe({
        next: (reqs: ExitRequest[]) => {
          this.allRequests = reqs;
          this.loading = false;
        },
        error: () => (this.loading = false)
      });
    } else {
      this.loading = false;
    }
  }

  viewDetails(req: ExitRequest): void {
    this.offboardingService.getExitRequestById(req.id).subscribe({
      next: (full: ExitRequest) => (this.selectedRequest = full)
    });
  }

  submitResignation(): void {
    if (!this.resignationForm.reason) return;
    this.offboardingService.submitResignation(this.resignationForm).subscribe({
      next: (saved: ExitRequest) => {
        this.showResignationModal = false;
        this.loadData();
        this.snackBar.open('Resignation submitted for HR review.', 'OK', { duration: 3000 });
      },
      error: (err: any) => {
        this.snackBar.open(err.error?.message || 'Failed to submit resignation', 'Close', { duration: 4000 });
      }
    });
  }

  openReviewModal(req: ExitRequest): void {
    this.selectedRequest = req;
    this.reviewAction = {
      approved: true,
      remarks: '',
      agreedLastWorkingDay: req.lastWorkingDay
    };
    this.showReviewModal = true;
  }

  submitReviewAction(): void {
    if (!this.selectedRequest) return;
    this.offboardingService.processExitRequest(this.selectedRequest.id, this.reviewAction).subscribe({
      next: (updated: ExitRequest) => {
        this.showReviewModal = false;
        this.viewDetails(updated);
        this.loadData();
        this.snackBar.open('Resignation request processed.', 'OK', { duration: 3000 });
      }
    });
  }

  updateClearanceStatus(item: ClearanceItem, status: 'CLEARED' | 'WAIVED'): void {
    this.offboardingService.updateClearanceItem(item.id, { status, remarks: 'Verified by dept lead' }).subscribe({
      next: (updated: ClearanceItem) => {
        item.status = updated.status;
        item.clearedAt = updated.clearedAt;
        this.snackBar.open(`Checklist item marked as ${status}!`, 'OK', { duration: 2500 });
      }
    });
  }

  finalizeSettlement(settlementId: number): void {
    this.offboardingService.finalizeSettlement(settlementId).subscribe({
      next: (res: FnFSettlement) => {
        if (this.selectedRequest) {
          this.viewDetails(this.selectedRequest);
        }
        this.loadData();
        this.snackBar.open('Full & Final settlement finalized! Employee status set to EXITED.', 'OK', { duration: 4000 });
      },
      error: (err: any) => {
        this.snackBar.open(err.error?.message || 'Cannot finalize: pending checklist items.', 'Close', { duration: 4500 });
      }
    });
  }
}
