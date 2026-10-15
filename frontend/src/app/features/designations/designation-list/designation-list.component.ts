import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../../../shared/shared.module';
import { DesignationService } from '../../../core/services/designation.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Designation } from '../../../core/models/designation.model';

@Component({
  selector: 'app-designation-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SharedModule],
  templateUrl: './designation-list.component.html',
  styleUrls: ['./designation-list.component.scss']
})
export class DesignationListComponent implements OnInit {
  private fb = inject(FormBuilder);
  private designationService = inject(DesignationService);
  authService = inject(AuthService);
  private notify = inject(NotificationService);

  designations: Designation[] = [];
  isLoading = false;
  showForm = false;
  isEditing = false;
  editingId?: number;

  desigForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    level: [3, [Validators.min(1), Validators.max(20)]],
    description: ['', [Validators.maxLength(500)]]
  });

  displayedColumns: string[] = ['level', 'title', 'description', 'createdAt', 'actions'];

  ngOnInit(): void {
    this.loadDesignations();
  }

  loadDesignations(): void {
    this.isLoading = true;
    this.designationService.getAllDesignations().subscribe({
      next: (data) => {
        this.isLoading = false;
        this.designations = data;
      },
      error: () => {
        this.isLoading = false;
        this.notify.error('Failed to load designations');
      }
    });
  }

  openCreateForm(): void {
    this.isEditing = false;
    this.editingId = undefined;
    this.desigForm.reset({ level: 3 });
    this.showForm = true;
  }

  openEditForm(d: Designation): void {
    this.isEditing = true;
    this.editingId = d.id;
    this.desigForm.patchValue({
      title: d.title,
      level: d.level,
      description: d.description
    });
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.desigForm.reset({ level: 3 });
  }

  saveDesignation(): void {
    if (this.desigForm.invalid) {
      this.desigForm.markAllAsTouched();
      return;
    }

    const payload = this.desigForm.value;
    if (this.isEditing && this.editingId) {
      this.designationService.updateDesignation(this.editingId, payload).subscribe({
        next: () => {
          this.notify.success('Designation updated successfully');
          this.cancelForm();
          this.loadDesignations();
        },
        error: (err) => this.notify.error(err.error?.message || 'Failed to update designation')
      });
    } else {
      this.designationService.createDesignation(payload).subscribe({
        next: () => {
          this.notify.success('Designation created successfully');
          this.cancelForm();
          this.loadDesignations();
        },
        error: (err) => this.notify.error(err.error?.message || 'Failed to create designation')
      });
    }
  }

  deleteDesignation(d: Designation): void {
    if (!d.id || !confirm(`Delete designation "${d.title}"?`)) return;

    this.designationService.deleteDesignation(d.id).subscribe({
      next: () => {
        this.notify.success('Designation deleted');
        this.loadDesignations();
      },
      error: (err) => this.notify.error(err.error?.message || 'Cannot delete designation')
    });
  }

  get isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  get isAdminOrHr(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  }
}
