import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs/operators';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.waitForInit().pipe(
    map(() => {
      const expectedRoles = route.data?.['roles'] as string[];
      if (!expectedRoles || expectedRoles.length === 0) {
        return true;
      }

      if (authService.hasAnyRole(expectedRoles)) {
        return true;
      }

      // User lacks permission: send them to the portal, which every logged-in role can open.
      // (Redirecting to another role-guarded page such as /employees caused an infinite loop for EMPLOYEE.)
      return router.createUrlTree(['/portal']);
    })
  );
};
