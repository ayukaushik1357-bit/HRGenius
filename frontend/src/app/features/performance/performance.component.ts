import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSliderModule } from '@angular/material/slider';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { PerformanceService } from '../../core/services/performance.service';
import { AuthService } from '../../core/services/auth.service';
import { ReviewCycle, Goal, PerformanceReview } from '../../core/models/performance.model';

@Component({
  selector: 'app-performance',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatTableModule,
    MatProgressBarModule,
    MatSliderModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl: './performance.component.html',
  styleUrls: ['./performance.component.scss']
})
export class PerformanceComponent implements OnInit {
  cycles: ReviewCycle[] = [];
  selectedCycleId?: number;
  goals: Goal[] = [];
  myReview: PerformanceReview | null = null;
  teamReviews: PerformanceReview[] = [];
  cycleReviews: PerformanceReview[] = [];

  isAdminOrHr: boolean = false;
  isManager: boolean = false;
  loading: boolean = true;

  // New Goal Modal state
  showNewGoalModal: boolean = false;
  newGoal = { title: '', description: '', weightage: 20, progressPercentage: 0 };

  // New Cycle Modal state
  showNewCycleModal: boolean = false;
  newCycle = { name: '', startDate: '', endDate: '', status: 'ACTIVE' };

  // Appraisal Modal state
  selectedReviewForAction: PerformanceReview | null = null;
  appraisalMode: 'SELF' | 'MANAGER' | 'PEER' = 'SELF';
  appraisalRating: number = 4.0;
  appraisalComments: string = '';

  constructor(
    private performanceService: PerformanceService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const roles = this.authService.currentUser()?.roles || [];
    this.isAdminOrHr = roles.includes('ROLE_ADMIN') || roles.includes('ROLE_HR_MANAGER');
    this.isManager = roles.includes('ROLE_MANAGER') || this.isAdminOrHr;

    this.loadCycles();
  }

  loadCycles(): void {
    this.performanceService.getCycles().subscribe({
      next: (cycles: ReviewCycle[]) => {
        this.cycles = cycles;
        if (cycles.length > 0) {
          this.selectedCycleId = cycles[0].id;
          this.loadCycleData();
        } else {
          this.loading = false;
        }
      },
      error: () => (this.loading = false)
    });
  }

  loadCycleData(): void {
    if (!this.selectedCycleId) return;
    this.loading = true;

    // Load Goals
    this.performanceService.getGoals(undefined, this.selectedCycleId).subscribe({
      next: (goals: Goal[]) => (this.goals = goals)
    });

    // Load My Review
    this.performanceService.getMyReview(this.selectedCycleId).subscribe({
      next: (rev: PerformanceReview) => (this.myReview = rev),
      error: () => (this.myReview = null)
    });

    // Load Team Reviews if Manager
    if (this.isManager) {
      this.performanceService.getTeamReviews().subscribe({
        next: (revs: PerformanceReview[]) => (this.teamReviews = revs)
      });
    }

    // Load All Cycle Reviews if Admin/HR
    if (this.isAdminOrHr) {
      this.performanceService.getReviewsForCycle(this.selectedCycleId).subscribe({
        next: (revs: PerformanceReview[]) => {
          this.cycleReviews = revs;
          this.loading = false;
        },
        error: () => (this.loading = false)
      });
    } else {
      this.loading = false;
    }
  }

  onCycleChange(): void {
    this.loadCycleData();
  }

  updateGoalProgress(goal: Goal, progress: number): void {
    this.performanceService.updateGoalProgress(goal.id, progress).subscribe({
      next: (updated: Goal) => {
        goal.progressPercentage = updated.progressPercentage;
        this.snackBar.open('Goal progress updated!', 'OK', { duration: 2500 });
      }
    });
  }

  openCreateGoal(): void {
    this.newGoal = { title: '', description: '', weightage: 25, progressPercentage: 0 };
    this.showNewGoalModal = true;
  }

  saveGoal(): void {
    if (!this.selectedCycleId || !this.newGoal.title) return;
    this.performanceService.createGoal({
      reviewCycleId: this.selectedCycleId,
      title: this.newGoal.title,
      description: this.newGoal.description,
      weightage: this.newGoal.weightage,
      progressPercentage: this.newGoal.progressPercentage
    }).subscribe({
      next: (saved: Goal) => {
        this.goals.push(saved);
        this.showNewGoalModal = false;
        this.snackBar.open('Goal added successfully!', 'OK', { duration: 2500 });
      }
    });
  }

  openCreateCycle(): void {
    this.newCycle = {
      name: 'Quarterly Review ' + new Date().getFullYear(),
      startDate: new Date().toISOString().slice(0, 10),
      endDate: new Date(Date.now() + 90 * 86400000).toISOString().slice(0, 10),
      status: 'ACTIVE'
    };
    this.showNewCycleModal = true;
  }

  saveCycle(): void {
    if (!this.newCycle.name) return;
    this.performanceService.createCycle(this.newCycle).subscribe({
      next: (saved: ReviewCycle) => {
        this.cycles.push(saved);
        this.selectedCycleId = saved.id;
        this.showNewCycleModal = false;
        this.loadCycleData();
        this.snackBar.open('Appraisal cycle created and initialized for staff!', 'OK', { duration: 3000 });
      }
    });
  }

  openAppraisalModal(rev: PerformanceReview, mode: 'SELF' | 'MANAGER' | 'PEER'): void {
    this.selectedReviewForAction = rev;
    this.appraisalMode = mode;
    this.appraisalRating = (mode === 'SELF' ? rev.selfRating : rev.managerRating) || 4.0;
    this.appraisalComments = (mode === 'SELF' ? rev.selfComments : rev.managerComments) || '';
  }

  submitAppraisal(): void {
    if (!this.selectedReviewForAction) return;
    const revId = this.selectedReviewForAction.id;

    if (this.appraisalMode === 'SELF') {
      this.performanceService.submitSelfReview(revId, this.appraisalRating, this.appraisalComments).subscribe({
        next: (updated: PerformanceReview) => {
          this.myReview = updated;
          this.selectedReviewForAction = null;
          this.snackBar.open('Self-review submitted to manager!', 'OK', { duration: 3000 });
        }
      });
    } else if (this.appraisalMode === 'MANAGER') {
      this.performanceService.submitManagerReview(revId, this.appraisalRating, this.appraisalComments).subscribe({
        next: (updated: PerformanceReview) => {
          this.selectedReviewForAction = null;
          this.loadCycleData();
          this.snackBar.open(`Appraisal finalized with score: ${updated.finalRating}/5.0`, 'OK', { duration: 3500 });
        }
      });
    } else if (this.appraisalMode === 'PEER') {
      this.performanceService.submitPeerFeedback({
        performanceReviewId: revId,
        rating: this.appraisalRating,
        feedback: this.appraisalComments
      }).subscribe({
        next: () => {
          this.selectedReviewForAction = null;
          this.snackBar.open('360 Peer feedback submitted!', 'OK', { duration: 3000 });
        }
      });
    }
  }
}
