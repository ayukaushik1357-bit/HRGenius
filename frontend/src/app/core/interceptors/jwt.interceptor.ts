import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';
import { Router } from '@angular/router';

let isRefreshing = false;

/** Public auth endpoints: no bearer token, no refresh-and-retry, no global error toast. */
const PUBLIC_AUTH = ['/auth/login', '/auth/refresh', '/auth/register', '/auth/google', '/auth/password-reset', '/auth/config'];
const isPublicAuth = (url: string) => PUBLIC_AUTH.some(path => url.includes(path));
let refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notificationService = inject(NotificationService);

  let clonedReq = req.clone({
    withCredentials: true
  });

  const token = authService.getAccessToken();
  if (token && !isPublicAuth(req.url)) {
    clonedReq = clonedReq.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (isPublicAuth(req.url)) {
        return throwError(() => error);
      }
      if (error.status === 403) {
        notificationService.error("You don't have permission");
        return throwError(() => error);
      }
      if (error.status === 401) {
        return handle401Error(clonedReq, next, authService, router);
      }
      return throwError(() => error);
    })
  );
};

function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  router: Router
) {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap(res => {
        isRefreshing = false;
        refreshTokenSubject.next(res.accessToken);
        return next(req.clone({
          setHeaders: {
            Authorization: `Bearer ${res.accessToken}`
          }
        }));
      }),
      catchError(refreshErr => {
        isRefreshing = false;
        const failedSubject = refreshTokenSubject;
        refreshTokenSubject = new BehaviorSubject<string | null>(null);
        failedSubject.error(refreshErr);
        authService.logout();
        router.navigate(['/auth/login']);
        return throwError(() => refreshErr);
      })
    );
  } else {
    return refreshTokenSubject.pipe(
      filter(token => token != null),
      take(1),
      switchMap(token => {
        return next(req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        }));
      }),
      catchError(err => throwError(() => err))
    );
  }
}
