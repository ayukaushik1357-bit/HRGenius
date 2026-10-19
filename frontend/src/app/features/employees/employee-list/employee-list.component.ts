import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { SharedModule } from '../../../shared/shared.module';
import { EmployeeService } from '../../../core/services/employee.service';
import { DepartmentService } from '../../../core/services/department.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Employee } from '../../../core/models/employee.model';
import { Department } from '../../../core/models/department.model';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, SharedModule],
  templateUrl: './employee-list.component.html',
  styleUrls: ['./employee-list.component.scss']
})
export class EmployeeListComponent implements OnInit {
  private employeeService = inject(EmployeeService);
  private departmentService = inject(DepartmentService);
  authService = inject(AuthService);
  private notify = inject(NotificationService);

  employees: Employee[] = [];
  departments: Department[] = [];

  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  isLoading = false;

  // Filters
  searchName = '';
  selectedDepartmentId: number | undefined;
  selectedStatus = '';

  displayedColumns: string[] = [
    'code', 'name', 'email', 'department', 'designation', 'status', 'actions'
  ];

  ngOnInit(): void {
    this.loadDepartments();
    this.loadEmployees();
  }

  loadDepartments(): void {
    this.departmentService.getAllDepartments().subscribe({
      next: (data) => this.departments = data,
      error: () => this.notify.error('Failed to load departments')
    });
  }

  loadEmployees(): void {
    this.isLoading = true;
    this.employeeService.searchEmployees({
      name: this.searchName.trim() || undefined,
      departmentId: this.selectedDepartmentId || undefined,
      status: this.selectedStatus || undefined,
      page: this.pageIndex,
      size: this.pageSize,
      sortBy: 'id',
      sortDir: 'desc'
    }).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.employees = res.content;
        this.totalElements = res.totalElements;
      },
      error: () => {
        this.isLoading = false;
        this.notify.error('Failed to load employees');
      }
    });
  }

  onFilterChange(): void {
    this.pageIndex = 0;
    this.loadEmployees();
  }

  resetFilters(): void {
    this.searchName = '';
    this.selectedDepartmentId = undefined;
    this.selectedStatus = '';
    this.pageIndex = 0;
    this.loadEmployees();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadEmployees();
  }

  deleteEmployee(emp: Employee): void {
    if (!confirm(`Delete ${emp.firstName} ${emp.lastName}?\n\nThey are removed from the employee list and can no longer sign in. Their payslips, leave and attendance history are kept.`)) {
      return;
    }

    this.employeeService.deleteEmployee(emp.id).subscribe({
      next: () => {
        this.notify.success('Employee deleted successfully');
        this.loadEmployees();
      },
      error: (err) => {
        this.notify.error(err.error?.message || 'Failed to delete employee');
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'badge-success';
      case 'ON_NOTICE': return 'badge-warning';
      case 'EXITED': return 'badge-danger';
      default: return 'badge-primary';
    }
  }

  get isAdminOrHr(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  }

  get isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }
}
