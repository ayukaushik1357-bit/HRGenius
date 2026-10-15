import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../../shared/shared.module';
import { EmployeeService } from '../../../core/services/employee.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Employee, DocumentMetadata } from '../../../core/models/employee.model';

@Component({
  selector: 'app-employee-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, SharedModule],
  templateUrl: './employee-profile.component.html',
  styleUrls: ['./employee-profile.component.scss']
})
export class EmployeeProfileComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private employeeService = inject(EmployeeService);
  authService = inject(AuthService);
  private notify = inject(NotificationService);

  employee?: Employee;
  documents: DocumentMetadata[] = [];
  isLoading = false;
  isUploading = false;

  selectedDocType = 'ID_PROOF';
  selectedFile: File | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadEmployee(+id);
    }
  }

  loadEmployee(id: number): void {
    this.isLoading = true;
    this.employeeService.getEmployeeById(id).subscribe({
      next: (emp) => {
        this.isLoading = false;
        this.employee = emp;
        this.documents = emp.documents || [];
      },
      error: () => {
        this.isLoading = false;
        this.notify.error('Employee not found');
        this.router.navigate(['/employees']);
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  uploadDocument(): void {
    if (!this.employee || !this.selectedFile) {
      this.notify.error('Please select a file to upload');
      return;
    }

    this.isUploading = true;
    this.employeeService.uploadDocument(this.employee.id, this.selectedDocType, this.selectedFile).subscribe({
      next: (doc) => {
        this.isUploading = false;
        this.notify.success('Document uploaded successfully');
        this.documents.push(doc);
        this.selectedFile = null;
        // reset input
        const fileInput = document.getElementById('fileUploadInput') as HTMLInputElement;
        if (fileInput) fileInput.value = '';
      },
      error: (err) => {
        this.isUploading = false;
        this.notify.error(err.error?.message || 'Failed to upload document');
      }
    });
  }

  downloadDocument(doc: DocumentMetadata): void {
    this.employeeService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.originalFileName;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.notify.error('Failed to download document')
    });
  }

  deleteDocument(docId: number): void {
    if (!confirm('Are you sure you want to delete this document?')) return;

    this.employeeService.deleteDocument(docId).subscribe({
      next: () => {
        this.notify.success('Document deleted');
        this.documents = this.documents.filter(d => d.id !== docId);
      },
      error: () => this.notify.error('Failed to delete document')
    });
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'ACTIVE': return 'badge-success';
      case 'ON_NOTICE': return 'badge-warning';
      case 'EXITED': return 'badge-danger';
      default: return 'badge-primary';
    }
  }

  formatFileSize(bytes?: number): string {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  get isAdminOrHr(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  }
}
