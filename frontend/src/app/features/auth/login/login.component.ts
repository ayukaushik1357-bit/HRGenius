import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { SharedModule } from '../../../shared/shared.module';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SharedModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private notify = inject(NotificationService);

  loginForm: FormGroup = this.fb.group({
    username: ['admin', [Validators.required]],
    password: ['Admin@123', [Validators.required]]
  });

  isLoading = false;
  hidePassword = true;
  errorMessage = '';

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.isLoading = false;
        this.notify.success('Welcome back to HRGenius!');
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/employees';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Invalid username or password';
        this.notify.error(this.errorMessage);
      }
    });
  }

  fillAdmin(): void {
    this.loginForm.patchValue({ username: 'admin', password: 'Admin@123' });
  }
}
