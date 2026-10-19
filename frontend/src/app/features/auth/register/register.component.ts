import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { AuthConfig } from '../../../core/models/auth.model';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';
import { GoogleButtonComponent } from '../google-button/google-button.component';
import { passwordRules, strongPassword } from '../password-rules';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthFrameComponent, GoogleButtonComponent],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, strongPassword]]
  });

  readonly rules = passwordRules;
  config: AuthConfig | null = null;
  isLoading = false;
  showPassword = false;
  errorMessage = '';
  doneMessage = '';

  ngOnInit(): void {
    this.authService.getConfig().subscribe({
      next: config => (this.config = config),
      error: () => (this.config = { signupEnabled: true, googleEnabled: false })
    });
  }

  invalid(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  passes(test: (value: string) => boolean): boolean {
    return test(this.form.controls.password.value);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.register(this.form.getRawValue()).subscribe({
      next: res => {
        this.isLoading = false;
        this.doneMessage = res.message;
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = err.status === 0
          ? 'Cannot reach the server. Try again in a moment.'
          : err.error?.message ?? 'The request could not be sent. Try again.';
      }
    });
  }

  onGoogleCredential(credential: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.googleLogin(credential).subscribe({
      next: res => {
        this.isLoading = false;
        this.doneMessage = res.status === 'SIGNED_IN'
          ? 'You already have an account. Go to sign in.'
          : res.message ?? 'Your request has been sent for approval.';
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message ?? 'Google sign-up failed. Try again.';
      }
    });
  }
}
