import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs/operators';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.waitForInit().pipe(
    map(() => {
      if (authService.isAuthenticated()) {
        if (authService.mustChangePassword() && !state.url.includes('/auth/change-password')) {
          router.navigate(['/auth/change-password']);
          return false;
        }
        return true;
      }
      router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
      return false;
    })
  );
};
