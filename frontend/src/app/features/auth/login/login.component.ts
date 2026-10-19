import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { AuthConfig, AuthResponse } from '../../../core/models/auth.model';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';
import { GoogleButtonComponent } from '../google-button/google-button.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthFrameComponent, GoogleButtonComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  config: AuthConfig | null = null;
  isLoading = false;
  showPassword = false;
  errorMessage = '';
  infoMessage = '';

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('reset') === 'done') {
      this.infoMessage = 'Your password has been changed. Sign in with the new one.';
    }
    this.authService.getConfig().subscribe({
      next: config => (this.config = config),
      error: () => (this.config = { signupEnabled: true, googleEnabled: false })
    });
  }

  fieldInvalid(name: 'username' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.login(this.form.getRawValue()).subscribe({
      next: res => this.afterSignIn(res),
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = this.describeError(err);
      }
    });
  }

  onGoogleCredential(credential: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.infoMessage = '';
    this.authService.googleLogin(credential).subscribe({
      next: res => {
        if (res.status === 'SIGNED_IN' && res.auth) {
          this.afterSignIn(res.auth);
        } else {
          this.isLoading = false;
          this.infoMessage = res.message ?? 'Your request has been sent for approval.';
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = this.describeError(err);
      }
    });
  }

  private afterSignIn(res: AuthResponse): void {
    this.isLoading = false;
    if (res.mustChangePassword) {
      this.router.navigate(['/auth/change-password']);
      return;
    }
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/portal';
    this.router.navigateByUrl(returnUrl);
  }

  private describeError(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return 'Cannot reach the server. Check that the backend is running, then try again.';
      case 401: return 'That email, username or password is not right. Check them and try again.';
      case 403: return err.error?.message ?? 'Your account is waiting for administrator approval.';
      case 423: return 'Too many wrong passwords. Your account is locked for 15 minutes, or reset your password now.';
      case 429: return 'Too many attempts from this network. Wait a minute and try again.';
      default: return err.error?.message ?? 'Sign-in failed. Try again.';
    }
  }
}
