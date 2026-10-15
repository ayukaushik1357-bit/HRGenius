import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { OnboardingService } from '../../core/services/onboarding.service';
import { EmployeeService } from '../../core/services/employee.service';
import { NotificationService } from '../../core/services/notification.service';
import { OnboardingTemplate, OnboardingProgress, OnboardingTask } from '../../core/models/onboarding.model';
import { Employee } from '../../core/models/employee.model';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatCheckboxModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './onboarding.component.html',
  styleUrls: ['./onboarding.component.scss']
})
export class OnboardingComponent implements OnInit {
  employees = signal<Employee[]>([]);
  selectedEmployee = signal<Employee | null>(null);
  progress = signal<OnboardingProgress | null>(null);
  templates = signal<OnboardingTemplate[]>([]);

  showTemplateModal = false;
  templateForm!: FormGroup;

  categories = ['DOCUMENTS', 'IT_SETUP', 'INDUCTION', 'TRAINING'] as const;

  constructor(
    private onboardingService: OnboardingService,
    private employeeService: EmployeeService,
    private notify: NotificationService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initTemplateForm();
    this.loadEmployees();
    this.loadTemplates();
  }

  initTemplateForm(): void {
    this.templateForm = this.fb.group({
      name: ['', Validators.required],
      description: ['']
    });
  }

  loadEmployees(): void {
    this.employeeService.searchEmployees({ size: 50 }).subscribe(res => {
      this.employees.set(res.content);
      if (res.content.length > 0) {
        this.selectEmployee(res.content[0]);
      }
    });
  }

  loadTemplates(): void {
    this.onboardingService.getTemplates().subscribe(t => this.templates.set(t));
  }

  selectEmployee(emp: Employee): void {
    this.selectedEmployee.set(emp);
    this.onboardingService.getEmployeeProgress(emp.id).subscribe({
      next: p => this.progress.set(p),
      error: () => this.progress.set(null)
    });
  }

  getTasksByCategory(category: string): OnboardingTask[] {
    if (!this.progress()?.tasks) return [];
    return this.progress()!.tasks.filter(t => t.category === category);
  }

  toggleTask(task: OnboardingTask): void {
    const nextStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    this.onboardingService.updateTaskStatus(task.id, nextStatus).subscribe({
      next: updated => {
        if (this.selectedEmployee()) {
          this.selectEmployee(this.selectedEmployee()!);
        }
        this.notify.success(`Task marked as ${nextStatus}`);
      },
      error: () => this.notify.error('Failed to update task status')
    });
  }

  openTemplateModal(): void {
    this.templateForm.reset();
    this.showTemplateModal = true;
  }

  closeTemplateModal(): void {
    this.showTemplateModal = false;
  }

  submitTemplate(): void {
    if (this.templateForm.invalid) return;
    this.onboardingService.createTemplate(this.templateForm.value).subscribe({
      next: (created) => {
        this.templates.update(list => [...list, created]);
        this.notify.success('Template created!');
        this.closeTemplateModal();
      },
      error: () => this.notify.error('Failed to create template')
    });
  }

  assignTemplateToSelected(templateId: number): void {
    const emp = this.selectedEmployee();
    if (!emp) return;
    this.onboardingService.assignTemplate(emp.id, templateId).subscribe({
      next: () => {
        this.notify.success('Checklist template assigned to employee!');
        this.selectEmployee(emp);
      },
      error: () => this.notify.error('Failed to assign template')
    });
  }
}
