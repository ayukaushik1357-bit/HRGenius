import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { SharedModule } from '../../../shared/shared.module';
import { EmployeeService } from '../../../core/services/employee.service';
import { DepartmentService } from '../../../core/services/department.service';
import { DesignationService } from '../../../core/services/designation.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Department } from '../../../core/models/department.model';
import { Designation } from '../../../core/models/designation.model';
import { Employee } from '../../../core/models/employee.model';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, SharedModule],
  templateUrl: './employee-form.component.html',
  styleUrls: ['./employee-form.component.scss']
})
export class EmployeeFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private employeeService = inject(EmployeeService);
  private departmentService = inject(DepartmentService);
  private designationService = inject(DesignationService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  isEditMode = false;
  employeeId?: number;
  isLoading = false;

  departments: Department[] = [];
  designations: Designation[] = [];
  managers: Employee[] = [];

  employeeForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    phone: ['', [Validators.maxLength(20)]],
    dateOfBirth: [''],
    gender: ['MALE'],
    joiningDate: ['', [Validators.required]],
    departmentId: [null],
    designationId: [null],
    reportingManagerId: [null],
    employmentType: ['FULL_TIME', [Validators.required]],
    status: ['ACTIVE', [Validators.required]],
    salaryCTC: [null, [Validators.min(0)]],

    // Address
    line1: ['', [Validators.required]],
    city: ['', [Validators.required]],
    state: ['', [Validators.required]],
    country: ['USA', [Validators.required]],
    zipCode: ['', [Validators.required]],

    // Emergency Contact
    contactName: ['', [Validators.required]],
    relationship: ['', [Validators.required]],
    contactPhone: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadDropdownData();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.employeeId = +idParam;
      this.loadEmployee(this.employeeId);
    }
  }

  loadDropdownData(): void {
    this.departmentService.getAllDepartments().subscribe(d => this.departments = d);
    this.designationService.getAllDesignations().subscribe(d => this.designations = d);
    this.employeeService.searchEmployees({ size: 100 }).subscribe(res => {
      this.managers = res.content.filter(e => !this.employeeId || e.id !== this.employeeId);
    });
  }

  loadEmployee(id: number): void {
    this.isLoading = true;
    this.employeeService.getEmployeeById(id).subscribe({
      next: (emp) => {
        this.isLoading = false;
        const addr = emp.addresses && emp.addresses.length > 0 ? emp.addresses[0] : null;
        const contact = emp.emergencyContacts && emp.emergencyContacts.length > 0 ? emp.emergencyContacts[0] : null;

        this.employeeForm.patchValue({
          firstName: emp.firstName,
          lastName: emp.lastName,
          email: emp.email,
          phone: emp.phone,
          dateOfBirth: emp.dateOfBirth,
          gender: emp.gender || 'MALE',
          joiningDate: emp.joiningDate,
          departmentId: emp.departmentId,
          designationId: emp.designationId,
          reportingManagerId: emp.reportingManagerId,
          employmentType: emp.employmentType,
          status: emp.status,
          salaryCTC: emp.salaryCTC,

          line1: addr?.line1 || '',
          city: addr?.city || '',
          state: addr?.state || '',
          country: addr?.country || 'USA',
          zipCode: addr?.zipCode || '',

          contactName: contact?.name || '',
          relationship: contact?.relationship || '',
          contactPhone: contact?.phone || ''
        });
      },
      error: () => {
        this.isLoading = false;
        this.notify.error('Failed to load employee details');
      }
    });
  }

  onSubmit(): void {
    if (this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      this.notify.error('Please fill in all required fields');
      return;
    }

    const val = this.employeeForm.value;

    const requestPayload = {
      firstName: val.firstName,
      lastName: val.lastName,
      email: val.email,
      phone: val.phone,
      dateOfBirth: val.dateOfBirth ? val.dateOfBirth : null,
      gender: val.gender,
      joiningDate: val.joiningDate,
      departmentId: val.departmentId || null,
      designationId: val.designationId || null,
      reportingManagerId: val.reportingManagerId || null,
      employmentType: val.employmentType,
      status: val.status,
      salaryCTC: val.salaryCTC,
      addresses: [{
        addressType: 'PERMANENT',
        line1: val.line1,
        city: val.city,
        state: val.state,
        country: val.country,
        zipCode: val.zipCode
      }],
      emergencyContacts: [{
        name: val.contactName,
        relationship: val.relationship,
        phone: val.contactPhone
      }]
    };

    this.isLoading = true;

    if (this.isEditMode && this.employeeId) {
      this.employeeService.updateEmployee(this.employeeId, requestPayload as any).subscribe({
        next: (saved) => {
          this.isLoading = false;
          this.notify.success('Employee updated successfully');
          this.router.navigate(['/employees', saved.id]);
        },
        error: (err) => {
          this.isLoading = false;
          this.notify.error(err.error?.message || 'Failed to update employee');
        }
      });
    } else {
      this.employeeService.createEmployee(requestPayload as any).subscribe({
        next: (saved) => {
          this.isLoading = false;
          this.notify.success('Employee created successfully');
          this.router.navigate(['/employees', saved.id]);
        },
        error: (err) => {
          this.isLoading = false;
          this.notify.error(err.error?.message || 'Failed to create employee');
        }
      });
    }
  }
}
