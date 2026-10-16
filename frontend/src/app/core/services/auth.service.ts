import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError, of } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthResponse, CurrentUser, LoginRequest } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private readonly TOKEN_KEY = 'hrgenius_access_token';
  private readonly USER_KEY = 'hrgenius_current_user';

  private accessTokenSignal = signal<string | null>(this.getInitialToken());
  private currentUserSignal = signal<CurrentUser | null>(this.getInitialUser());

  readonly accessToken = computed(() => this.accessTokenSignal());
  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isAuthenticated = computed(() => !!this.accessTokenSignal());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  private getInitialToken(): string | null {
    try {
      return sessionStorage.getItem(this.TOKEN_KEY);
    } catch {
      return null;
    }
  }

  private getInitialUser(): CurrentUser | null {
    try {
      const data = sessionStorage.getItem(this.USER_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
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

  private setSession(authResponse: AuthResponse): void {
    const user: CurrentUser = {
      username: authResponse.username,
      email: authResponse.email,
      roles: authResponse.roles
    };
    this.accessTokenSignal.set(authResponse.accessToken);
    this.currentUserSignal.set(user);
    try {
      sessionStorage.setItem(this.TOKEN_KEY, authResponse.accessToken);
      sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
    } catch {
      // Ignore storage errors in restricted contexts
    }
  }

  private clearSession(): void {
    this.accessTokenSignal.set(null);
    this.currentUserSignal.set(null);
    try {
      sessionStorage.removeItem(this.TOKEN_KEY);
      sessionStorage.removeItem(this.USER_KEY);
    } catch {
      // Ignore
    }
  }
}
