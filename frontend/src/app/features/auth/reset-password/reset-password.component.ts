import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';
import { passwordRules, strongPassword } from '../password-rules';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthFrameComponent],
  template: `
    <app-auth-frame heading="Choose a new password">
      @if (!token) {
        <div class="alert error" role="alert">This page needs the link from your email. Request a new one.</div>
        <p class="switch"><a class="inline-link" routerLink="/auth/forgot-password">Request a reset link</a></p>
      } @else {
        <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
          <div class="field">
            <label for="password">New password</label>
            <div class="input-wrap">
              <input id="password" [type]="showPassword ? 'text' : 'password'" formControlName="password"
                     autocomplete="new-password" aria-describedby="password-rules"
                     [attr.aria-invalid]="invalid('password')">
              <button type="button" class="toggle" (click)="showPassword = !showPassword"
                      [attr.aria-pressed]="showPassword" aria-controls="password">
                {{ showPassword ? 'Hide' : 'Show' }}
              </button>
            </div>
            <ul class="rules" id="password-rules">
              @for (rule of rules; track rule.label) {
                <li [class.met]="rule.test(form.controls.password.value)">{{ rule.label }}</li>
              }
            </ul>
          </div>
          <div class="field">
            <label for="confirm">Type it again</label>
            <input id="confirm" [type]="showPassword ? 'text' : 'password'" formControlName="confirm"
                   autocomplete="new-password" [attr.aria-invalid]="mismatch()">
            @if (mismatch()) { <p class="field-error">The two passwords are different.</p> }
          </div>
          @if (errorMessage) { <div class="alert error" role="alert">{{ errorMessage }}</div> }
          <button class="submit" type="submit" [disabled]="isLoading">
            {{ isLoading ? 'Saving…' : 'Save new password' }}
          </button>
        </form>
        <p class="switch"><a class="inline-link" routerLink="/auth/login">Back to sign in</a></p>
      }
    </app-auth-frame>
  `,
  styleUrls: ['../register/register.component.scss']
})
export class ResetPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');
  readonly rules = passwordRules;
  readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required, strongPassword]],
    confirm: ['', [Validators.required]]
  });
  isLoading = false;
  showPassword = false;
  errorMessage = '';

  invalid(name: 'password' | 'confirm'): boolean {
    const c = this.form.controls[name];
    return c.invalid && c.touched;
  }

  mismatch(): boolean {
    const { password, confirm } = this.form.getRawValue();
    return this.form.controls.confirm.touched && confirm !== password;
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.mismatch() || !this.token) {
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.confirmPasswordReset(this.token, this.form.controls.password.value).subscribe({
      next: () => this.router.navigate(['/auth/login'], { queryParams: { reset: 'done' } }),
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message ?? 'The password could not be changed. Request a new link.';
      }
    });
  }
}
