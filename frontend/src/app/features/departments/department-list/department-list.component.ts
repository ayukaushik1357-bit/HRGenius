import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../../../shared/shared.module';
import { DepartmentService } from '../../../core/services/department.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Department } from '../../../core/models/department.model';

@Component({
  selector: 'app-department-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SharedModule],
  templateUrl: './department-list.component.html',
  styleUrls: ['./department-list.component.scss']
})
export class DepartmentListComponent implements OnInit {
  private fb = inject(FormBuilder);
  private departmentService = inject(DepartmentService);
  authService = inject(AuthService);
  private notify = inject(NotificationService);

  departments: Department[] = [];
  isLoading = false;
  showForm = false;
  isEditing = false;
  editingId?: number;

  deptForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    description: ['', [Validators.maxLength(500)]]
  });

  displayedColumns: string[] = ['code', 'name', 'description', 'createdAt', 'actions'];

  ngOnInit(): void {
    this.loadDepartments();
  }

  loadDepartments(): void {
    this.isLoading = true;
    this.departmentService.getAllDepartments().subscribe({
      next: (data) => {
        this.isLoading = false;
        this.departments = data;
      },
      error: () => {
        this.isLoading = false;
        this.notify.error('Failed to load departments');
      }
    });
  }

  openCreateForm(): void {
    this.isEditing = false;
    this.editingId = undefined;
    this.deptForm.reset();
    this.showForm = true;
  }

  openEditForm(dept: Department): void {
    this.isEditing = true;
    this.editingId = dept.id;
    this.deptForm.patchValue({
      name: dept.name,
      code: dept.code,
      description: dept.description
    });
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.deptForm.reset();
  }

  saveDepartment(): void {
    if (this.deptForm.invalid) {
      this.deptForm.markAllAsTouched();
      return;
    }

    const payload = this.deptForm.value;
    if (this.isEditing && this.editingId) {
      this.departmentService.updateDepartment(this.editingId, payload).subscribe({
        next: () => {
          this.notify.success('Department updated successfully');
          this.cancelForm();
          this.loadDepartments();
        },
        error: (err) => this.notify.error(err.error?.message || 'Failed to update department')
      });
    } else {
      this.departmentService.createDepartment(payload).subscribe({
        next: () => {
          this.notify.success('Department created successfully');
          this.cancelForm();
          this.loadDepartments();
        },
        error: (err) => this.notify.error(err.error?.message || 'Failed to create department')
      });
    }
  }

  deleteDepartment(dept: Department): void {
    if (!dept.id || !confirm(`Delete department "${dept.name}"?`)) return;

    this.departmentService.deleteDepartment(dept.id).subscribe({
      next: () => {
        this.notify.success('Department deleted');
        this.loadDepartments();
      },
      error: (err) => this.notify.error(err.error?.message || 'Cannot delete department')
    });
  }

  get isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  get isAdminOrHr(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  }
}
