import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError, of, firstValueFrom, from, map } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthConfig, AuthResponse, CurrentUser, GoogleLoginResponse, LoginRequest, RegisterRequest } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private accessTokenSignal = signal<string | null>(null);
  private currentUserSignal = signal<CurrentUser | null>(null);
  private isInitializedSignal = signal<boolean>(false);
  private initPromise: Promise<void> | null = null;

  readonly accessToken = computed(() => this.accessTokenSignal());
  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isAuthenticated = computed(() => !!this.accessTokenSignal());
  readonly isInitialized = computed(() => this.isInitializedSignal());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  initAuth(): Promise<void> {
    if (!this.initPromise) {
      this.initPromise = firstValueFrom(
        this.refreshToken().pipe(
          catchError(() => of(null))
        )
      ).then(() => {
        this.isInitializedSignal.set(true);
      });
    }
    return this.initPromise;
  }

  waitForInit(): Observable<boolean> {
    if (this.isInitializedSignal()) {
      return of(true);
    }
    return from(this.initAuth()).pipe(map(() => true));
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials, {
      withCredentials: true // allows browser to store HttpOnly refresh token cookie
    }).pipe(
      tap(res => {
        this.setSession(res);
      })
    );
  }

  /** Public settings for the sign-in page (sign-up enabled, Google client ID). */
  getConfig(): Observable<AuthConfig> {
    return this.http.get<AuthConfig>(`${this.apiUrl}/config`);
  }

  register(request: RegisterRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/register`, request);
  }

  requestPasswordReset(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/password-reset/request`, { email });
  }

  confirmPasswordReset(token: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/password-reset/confirm`, { token, newPassword });
  }

  /** Exchanges a Google ID token for an HRGenius session, or registers a pending sign-up. */
  googleLogin(credential: string): Observable<GoogleLoginResponse> {
    return this.http.post<GoogleLoginResponse>(`${this.apiUrl}/google`, { credential }, {
      withCredentials: true
    }).pipe(
      tap(res => {
        if (res.status === 'SIGNED_IN' && res.auth) {
          this.setSession(res.auth);
        }
      })
    );
  }

  refreshToken(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, {
      withCredentials: true // send HttpOnly cookie
    }).pipe(
      tap(res => {
        this.setSession(res);
      }),
      catchError(err => {
        this.clearSession();
        return throwError(() => err);
      })
    );
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true })
      .pipe(catchError(() => of(null)))
      .subscribe(() => {
        this.clearSession();
        this.router.navigate(['/auth/login']);
      });
  }

  hasRole(role: string): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    const roleWithPrefix = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return user.roles.includes(roleWithPrefix);
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  getAccessToken(): string | null {
    return this.accessTokenSignal();
  }

  mustChangePassword(): boolean {
    return !!this.currentUserSignal()?.mustChangePassword;
  }

  changePassword(currentPassword: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/change-password`, {
      currentPassword,
      newPassword
    }, { withCredentials: true }).pipe(
      tap(() => {
        const user = this.currentUserSignal();
        if (user) {
          this.currentUserSignal.set({
            ...user,
            mustChangePassword: false
          });
        }
      })
    );
  }

  private setSession(authResponse: AuthResponse): void {
    const user: CurrentUser = {
      username: authResponse.username,
      email: authResponse.email,
      roles: authResponse.roles,
      mustChangePassword: authResponse.mustChangePassword
    };
    this.accessTokenSignal.set(authResponse.accessToken);
    this.currentUserSignal.set(user);
  }

  private clearSession(): void {
    this.accessTokenSignal.set(null);
    this.currentUserSignal.set(null);
  }
}
