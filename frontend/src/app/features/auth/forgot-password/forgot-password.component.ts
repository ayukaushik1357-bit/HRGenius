import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthFrameComponent],
  template: `
    <app-auth-frame heading="Reset your password"
                    subheading="Enter the email on your account and we will send you a link to choose a new password.">
      @if (sentMessage) {
        <div class="alert success" role="status">{{ sentMessage }}</div>
        <p class="hint">The link works once and expires in 30 minutes. Check your spam folder if it does not arrive.</p>
      } @else {
        <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" autocomplete="email"
                   placeholder="name@company.com" [attr.aria-invalid]="invalid()">
            @if (invalid()) { <p class="field-error">Enter a valid email address.</p> }
          </div>
          @if (errorMessage) { <div class="alert error" role="alert">{{ errorMessage }}</div> }
          <button class="submit" type="submit" [disabled]="isLoading">
            {{ isLoading ? 'Sending…' : 'Send reset link' }}
          </button>
        </form>
      }
      <p class="switch"><a class="inline-link" routerLink="/auth/login">Back to sign in</a></p>
    </app-auth-frame>
  `,
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });
  isLoading = false;
  sentMessage = '';
  errorMessage = '';

  invalid(): boolean {
    const c = this.form.controls.email;
    return c.invalid && c.touched;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.requestPasswordReset(this.form.controls.email.value).subscribe({
      next: res => {
        this.isLoading = false;
        this.sentMessage = res.message;
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = err.status === 429
          ? 'Too many requests from this network. Wait a minute and try again.'
          : 'The link could not be sent. Try again in a moment.';
      }
    });
  }
}
