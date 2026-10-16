import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RecruitmentService } from '../../core/services/recruitment.service';
import { DepartmentService } from '../../core/services/department.service';
import { DesignationService } from '../../core/services/designation.service';
import { OnboardingService } from '../../core/services/onboarding.service';
import { NotificationService } from '../../core/services/notification.service';
import { JobPosting, Candidate, Application, Interview, Offer, HireCandidateRequest } from '../../core/models/recruitment.model';
import { Department } from '../../core/models/department.model';
import { Designation } from '../../core/models/designation.model';
import { OnboardingTemplate } from '../../core/models/onboarding.model';

@Component({
  selector: 'app-recruitment',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    MatProgressBarModule
  ],
  templateUrl: './recruitment.component.html',
  styleUrls: ['./recruitment.component.scss']
})
export class RecruitmentComponent implements OnInit {
  jobPostings = signal<JobPosting[]>([]);
  applications = signal<Application[]>([]);
  departments = signal<Department[]>([]);
  designations = signal<Designation[]>([]);
  onboardingTemplates = signal<OnboardingTemplate[]>([]);
  selectedJobId = signal<number | null>(null);
  selectedTabIndex = 0;
  isLoading = signal<boolean>(false);

  // Modal controls
  showJobModal = false;
  showCandidateModal = false;
  showInterviewModal = false;
  showOfferModal = false;
  showHireModal = false;

  activeApplication: Application | null = null;
  activeInterviews: Interview[] = [];

  jobForm!: FormGroup;
  candidateForm!: FormGroup;
  interviewForm!: FormGroup;
  offerForm!: FormGroup;
  hireForm!: FormGroup;

  stages = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFERED', 'HIRED', 'REJECTED'] as const;

  constructor(
    private recruitmentService: RecruitmentService,
    private departmentService: DepartmentService,
    private designationService: DesignationService,
    private onboardingService: OnboardingService,
    private notify: NotificationService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadData();
  }

  initForms(): void {
    this.jobForm = this.fb.group({
      title: ['', Validators.required],
      departmentId: [null, Validators.required],
      designationId: [null, Validators.required],
      location: ['Bengaluru, India (Hybrid)'],
      minExperienceYears: [2],
      maxExperienceYears: [6],
      openings: [1, [Validators.required, Validators.min(1)]],
      status: ['OPEN'],
      description: [''],
      requirements: ['']
    });

    this.candidateForm = this.fb.group({
      jobPostingId: [null, Validators.required],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      experienceYears: [3],
      currentCompany: [''],
      expectedSalaryCTC: [1200000],
      notes: ['']
    });

    this.interviewForm = this.fb.group({
      interviewerId: [1, Validators.required],
      scheduledAt: [new Date().toISOString().substring(0, 16), Validators.required],
      roundName: ['Technical Round 1', Validators.required],
      rating: [4],
      feedback: [''],
      status: ['SCHEDULED']
    });

    this.offerForm = this.fb.group({
      offerDate: [new Date().toISOString().substring(0, 10), Validators.required],
      joiningDate: [new Date(Date.now() + 30 * 86400000).toISOString().substring(0, 10), Validators.required],
      offeredSalaryCTC: [1400000, [Validators.required, Validators.min(1)]],
      status: ['PENDING'],
      remarks: ['Standard offer package']
    });

    this.hireForm = this.fb.group({
      employeeCode: ['EMP' + Math.floor(100 + Math.random() * 900), Validators.required],
      departmentId: [null, Validators.required],
      designationId: [null, Validators.required],
      officialEmail: ['', [Validators.required, Validators.email]],
      initialPassword: ['Welcome@123', Validators.required],
      joiningDate: [new Date().toISOString().substring(0, 10), Validators.required],
      onboardingTemplateId: [null],
      basicSalary: [50000, Validators.required],
      hra: [25000, Validators.required],
      specialAllowance: [15000],
      otherAllowances: [5000]
    });
  }

  loadData(): void {
    this.isLoading.set(true);
    let pending = 5;
    const dec = () => {
      pending--;
      if (pending <= 0) {
        this.isLoading.set(false);
      }
    };

    this.recruitmentService.getJobPostings().subscribe({
      next: jobs => { this.jobPostings.set(jobs || []); dec(); },
      error: () => { dec(); }
    });
    this.recruitmentService.getApplications().subscribe({
      next: apps => { this.applications.set(apps || []); dec(); },
      error: () => { dec(); }
    });
    this.departmentService.getAllDepartments().subscribe({
      next: depts => { this.departments.set(depts || []); dec(); },
      error: () => { dec(); }
    });
    this.designationService.getAllDesignations().subscribe({
      next: desigs => { this.designations.set(desigs || []); dec(); },
      error: () => { dec(); }
    });
    this.onboardingService.getTemplates().subscribe({
      next: templates => { this.onboardingTemplates.set(templates || []); dec(); },
      error: () => { dec(); }
    });
  }

  getStageApplications(stage: string): Application[] {
    const list = this.applications().filter(a => a.stage === stage);
    const selected = this.selectedJobId();
    if (selected) {
      return list.filter(a => a.jobPostingId === selected);
    }
    return list;
  }

  filterByJob(jobId: number | null): void {
    this.selectedJobId.set(jobId);
  }

  viewCandidates(job: JobPosting): void {
    this.selectedJobId.set(job.id);
    this.selectedTabIndex = 0; // Automatically switch to Candidate Applied tab
    this.notify.info(`Showing candidates for: ${job.title}`);
  }

  getSelectedJobTitle(): string {
    const id = this.selectedJobId();
    if (!id) return '';
    const job = this.jobPostings().find(j => j.id === id);
    return job ? job.title : '';
  }

  moveStage(app: Application, newStage: string): void {
    this.recruitmentService.updateStage(app.id, newStage).subscribe({
      next: updated => {
        this.applications.update(list => list.map(a => a.id === updated.id ? updated : a));
        this.notify.success(`Candidate moved to ${newStage}`);
      },
      error: () => this.notify.error('Failed to update stage')
    });
  }

  // Job Posting CRUD
  openJobModal(): void {
    if (this.departments().length === 0) {
      this.departmentService.getAllDepartments().subscribe(depts => this.departments.set(depts || []));
    }
    if (this.designations().length === 0) {
      this.designationService.getAllDesignations().subscribe(desigs => this.designations.set(desigs || []));
    }
    this.jobForm.reset({
      location: 'Bengaluru, India (Hybrid)',
      minExperienceYears: 2,
      maxExperienceYears: 6,
      openings: 1,
      status: 'OPEN'
    });
    this.showJobModal = true;
  }

  closeJobModal(): void {
    this.showJobModal = false;
  }

  submitJob(): void {
    if (this.jobForm.invalid) return;
    this.recruitmentService.createJobPosting(this.jobForm.value).subscribe({
      next: created => {
        this.jobPostings.update(list => [created, ...list]);
        this.notify.success('Job posting created successfully');
        this.closeJobModal();
      },
      error: () => this.notify.error('Failed to create job posting')
    });
  }

  // Candidate + Application
  openCandidateModal(): void {
    if (this.jobPostings().length === 0) {
      this.recruitmentService.getJobPostings().subscribe(jobs => this.jobPostings.set(jobs || []));
    }
    this.candidateForm.reset({
      experienceYears: 3,
      expectedSalaryCTC: 1200000,
      jobPostingId: this.selectedJobId() || (this.jobPostings().length ? this.jobPostings()[0].id : null)
    });
    this.showCandidateModal = true;
  }

  closeCandidateModal(): void {
    this.showCandidateModal = false;
  }

  submitCandidate(): void {
    if (this.candidateForm.invalid) return;
    const v = this.candidateForm.value;
    const candidateData: Partial<Candidate> = {
      firstName: v.firstName,
      lastName: v.lastName,
      email: v.email,
      phone: v.phone,
      experienceYears: v.experienceYears,
      currentCompany: v.currentCompany,
      expectedSalaryCTC: v.expectedSalaryCTC
    };

    this.recruitmentService.createCandidate(candidateData).subscribe({
      next: candidate => {
        this.recruitmentService.createApplication(v.jobPostingId, candidate.id, v.notes).subscribe({
          next: app => {
            this.applications.update(list => [app, ...list]);
            this.notify.success('Candidate added to pipeline!');
            this.closeCandidateModal();
          },
          error: () => this.notify.error('Failed to link candidate to job application')
        });
      },
      error: (err) => this.notify.error(err.error?.message || 'Failed to create candidate')
    });
  }

  // Interview Modal
  openInterviewModal(app: Application): void {
    this.activeApplication = app;
    this.recruitmentService.getInterviews(app.id).subscribe(list => this.activeInterviews = list);
    this.interviewForm.patchValue({
      roundName: 'Technical Round ' + (this.activeInterviews.length + 1),
      scheduledAt: new Date(Date.now() + 86400000).toISOString().substring(0, 16)
    });
    this.showInterviewModal = true;
  }

  closeInterviewModal(): void {
    this.showInterviewModal = false;
    this.activeApplication = null;
  }

  scheduleInterview(): void {
    if (!this.activeApplication || this.interviewForm.invalid) return;
    const request: Partial<Interview> = {
      applicationId: this.activeApplication.id,
      interviewerId: Number(this.interviewForm.value.interviewerId),
      scheduledAt: this.interviewForm.value.scheduledAt,
      roundName: this.interviewForm.value.roundName,
      rating: this.interviewForm.value.rating,
      feedback: this.interviewForm.value.feedback
    };

    this.recruitmentService.scheduleInterview(request).subscribe({
      next: () => {
        this.notify.success('Interview scheduled!');
        this.loadData();
        this.closeInterviewModal();
      },
      error: () => this.notify.error('Failed to schedule interview')
    });
  }

  // Offer Modal
  openOfferModal(app: Application): void {
    this.activeApplication = app;
    this.recruitmentService.getOffer(app.id).subscribe({
      next: offer => {
        this.offerForm.patchValue({
          offerDate: offer.offerDate,
          joiningDate: offer.joiningDate,
          offeredSalaryCTC: offer.offeredSalaryCTC,
          status: offer.status,
          remarks: offer.remarks
        });
      },
      error: () => {
        this.offerForm.patchValue({
          offerDate: new Date().toISOString().substring(0, 10),
          joiningDate: new Date(Date.now() + 30 * 86400000).toISOString().substring(0, 10),
          offeredSalaryCTC: 1400000,
          status: 'PENDING'
        });
      }
    });
    this.showOfferModal = true;
  }

  closeOfferModal(): void {
    this.showOfferModal = false;
    this.activeApplication = null;
  }

  saveOffer(): void {
    if (!this.activeApplication || this.offerForm.invalid) return;
    const request: Partial<Offer> = {
      applicationId: this.activeApplication.id,
      ...this.offerForm.value
    };
    this.recruitmentService.saveOffer(request).subscribe({
      next: () => {
        this.notify.success('Offer saved and stage updated to OFFERED!');
        this.loadData();
        this.closeOfferModal();
      },
      error: () => this.notify.error('Failed to save offer')
    });
  }

  // Hire & Convert to Employee Modal
  openHireModal(app: Application): void {
    this.activeApplication = app;
    const jp = this.jobPostings().find(j => j.id === app.jobPostingId);
    this.hireForm.patchValue({
      employeeCode: 'EMP' + Math.floor(100 + Math.random() * 900),
      departmentId: jp?.departmentId || (this.departments().length ? this.departments()[0].id : null),
      designationId: jp?.designationId || (this.designations().length ? this.designations()[0].id : null),
      officialEmail: app.candidateEmail,
      initialPassword: 'Welcome@123',
      joiningDate: new Date().toISOString().substring(0, 10),
      onboardingTemplateId: this.onboardingTemplates().length ? this.onboardingTemplates()[0].id : null,
      basicSalary: 60000,
      hra: 30000,
      specialAllowance: 20000,
      otherAllowances: 5000
    });
    this.showHireModal = true;
  }

  closeHireModal(): void {
    this.showHireModal = false;
    this.activeApplication = null;
  }

  submitHire(): void {
    if (!this.activeApplication || this.hireForm.invalid) return;
    const req: HireCandidateRequest = {
      applicationId: this.activeApplication.id,
      ...this.hireForm.value
    };

    this.recruitmentService.hireCandidate(req).subscribe({
      next: (emp) => {
        this.notify.success(`Candidate hired! Employee record ${emp.employeeCode} created.`);
        this.loadData();
        this.closeHireModal();
      },
      error: (err) => this.notify.error(err.error?.message || 'Failed to complete hiring')
    });
  }
}
