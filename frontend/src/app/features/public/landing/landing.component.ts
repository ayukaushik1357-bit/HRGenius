import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface PayLine { label: string; amount: number; }
interface Stage { title: string; detail: string; }
interface Access { area: string; admin: string; hr: string; manager: string; employee: string; }

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent {
  readonly auth = inject(AuthService);
  readonly year = new Date().getFullYear();

  // A payslip for the seeded demo employee, using the same rules as the payroll engine
  readonly earnings: PayLine[] = [
    { label: 'Basic', amount: 55000 },
    { label: 'House rent allowance', amount: 27500 },
    { label: 'Special allowance', amount: 16500 },
    { label: 'Other allowances', amount: 11000 }
  ];
  readonly deductions: PayLine[] = [
    { label: 'Provident fund (12% of basic)', amount: 6600 },
    { label: 'Professional tax', amount: 200 },
    { label: 'Income tax (TDS)', amount: 8450 }
  ];
  readonly gross = this.earnings.reduce((sum, l) => sum + l.amount, 0);
  readonly totalDeductions = this.deductions.reduce((sum, l) => sum + l.amount, 0);
  readonly net = this.gross - this.totalDeductions;

  readonly stages: Stage[] = [
    { title: 'Hire', detail: 'Post a role, move candidates through interviews and send the offer.' },
    { title: 'Onboard', detail: 'The new joiner gets a login and a checklist the day they are hired.' },
    { title: 'Attendance and leave', detail: 'Punch in, apply for leave, and the right manager approves it.' },
    { title: 'Payroll', detail: 'PF, ESI, professional tax, TDS and loss of pay, with PDF payslips.' },
    { title: 'Appraisal', detail: 'Goals, a self review, the manager review and peer feedback.' },
    { title: 'Exit', detail: 'Resignation, clearance and the full and final settlement.' }
  ];

  readonly access: Access[] = [
    { area: 'Employee profiles', admin: 'Everyone', hr: 'Everyone', manager: 'Their team', employee: 'Their own' },
    { area: 'Salary, PAN and bank details', admin: 'Everyone', hr: 'Everyone', manager: 'Hidden', employee: 'Their own' },
    { area: 'Leave approval', admin: 'Everyone', hr: 'Everyone', manager: 'Their team', employee: 'No' },
    { area: 'Payslips', admin: 'Everyone', hr: 'Everyone', manager: 'No', employee: 'Their own' },
    { area: 'Payroll runs', admin: 'Yes', hr: 'Yes', manager: 'No', employee: 'No' },
    { area: 'Analytics', admin: 'Company', hr: 'Company', manager: 'Their team', employee: 'No' },
    { area: 'User accounts and audit log', admin: 'Yes', hr: 'Audit log', manager: 'No', employee: 'No' }
  ];

  inr(value: number): string {
    return value.toLocaleString('en-IN');
  }

  isLimited(value: string): boolean {
    return value === 'No' || value === 'Hidden';
  }
}
