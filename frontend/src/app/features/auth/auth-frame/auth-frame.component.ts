import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Shared two-panel frame for the sign-in, sign-up and password pages. */
@Component({
  selector: 'app-auth-frame',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="frame">
      <aside class="side">
        <a class="brand" routerLink="/" aria-label="HRGenius home">
          <span class="brand-mark" aria-hidden="true">HG</span>
          <span class="brand-name">HRGenius</span>
        </a>
        <div class="side-body">
          <p class="side-title">Your HR workspace</p>
          <ul class="side-points">
            <li>Leave, attendance and payslips for employees</li>
            <li>Approvals and team records for managers</li>
            <li>Payroll, hiring and reports for HR</li>
          </ul>
        </div>
        <a class="side-back" routerLink="/">Back to the HRGenius home page</a>
      </aside>
      <main class="panel">
        <div class="panel-inner">
          <h1>{{ heading }}</h1>
          @if (subheading) { <p class="sub">{{ subheading }}</p> }
          <ng-content />
        </div>
      </main>
    </div>
  `,
  styleUrls: ['./auth-frame.component.scss']
})
export class AuthFrameComponent {
  @Input({ required: true }) heading = '';
  @Input() subheading = '';
}
