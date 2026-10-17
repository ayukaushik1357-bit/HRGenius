import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { Router } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: any;
  let notificationServiceSpy: any;
  let routerSpy: any;

  beforeEach(() => {
    authServiceSpy = {
      getAccessToken: vi.fn(),
      refreshToken: vi.fn(),
      logout: vi.fn()
    };
    notificationServiceSpy = {
      error: vi.fn(),
      success: vi.fn(),
      info: vi.fn()
    };
    routerSpy = {
      navigate: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should attach Bearer token if available and not login/refresh', () => {
    authServiceSpy.getAccessToken.mockReturnValue('valid-token');

    http.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid-token');
    expect(req.request.withCredentials).toBe(true);
    req.flush({ ok: true });
  });

  it('should not refresh on 403, should show permission error and not logout', () => {
    authServiceSpy.getAccessToken.mockReturnValue('valid-token');

    let errorResponse: any;
    http.get('/api/forbidden').subscribe({
      next: () => {},
      error: (err) => { errorResponse = err; }
    });

    const req = httpMock.expectOne('/api/forbidden');
    req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });

    expect(notificationServiceSpy.error).toHaveBeenCalledWith("You don't have permission");
    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    expect(authServiceSpy.logout).not.toHaveBeenCalled();
    expect(errorResponse.status).toBe(403);
  });

  it('should refresh token on 401 and replay request with new token', () => {
    authServiceSpy.getAccessToken.mockReturnValue('expired-token');
    authServiceSpy.refreshToken.mockReturnValue(of({
      accessToken: 'new-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      username: 'user',
      email: 'user@example.com',
      roles: ['ROLE_EMPLOYEE']
    }));

    let result: any;
    http.get('/api/resource').subscribe({
      next: (res) => { result = res; }
    });

    // Initial request fails with 401
    const req1 = httpMock.expectOne('/api/resource');
    req1.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.refreshToken).toHaveBeenCalled();

    // Replayed request with new token
    const req2 = httpMock.expectOne('/api/resource');
    expect(req2.request.headers.get('Authorization')).toBe('Bearer new-token');
    req2.flush({ data: 'success' });

    expect(result).toEqual({ data: 'success' });
  });

  it('should queue multiple 401 requests and replay all with new token when refresh completes', () => {
    authServiceSpy.getAccessToken.mockReturnValue('expired-token');
    const refreshSubject = new Subject<any>();
    authServiceSpy.refreshToken.mockReturnValue(refreshSubject);

    let res1: any;
    let res2: any;
    http.get('/api/res1').subscribe(r => res1 = r);
    http.get('/api/res2').subscribe(r => res2 = r);

    const req1 = httpMock.expectOne('/api/res1');
    const req2 = httpMock.expectOne('/api/res2');

    // Both fail with 401 while refresh is in flight
    req1.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    req2.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    // refreshToken called only once
    expect(authServiceSpy.refreshToken).toHaveBeenCalledTimes(1);

    // Refresh now succeeds
    refreshSubject.next({
      accessToken: 'brand-new-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      username: 'user',
      email: 'user@example.com',
      roles: ['ROLE_EMPLOYEE']
    });
    refreshSubject.complete();

    // Both replayed with brand-new-token
    const retry1 = httpMock.expectOne('/api/res1');
    const retry2 = httpMock.expectOne('/api/res2');
    expect(retry1.request.headers.get('Authorization')).toBe('Bearer brand-new-token');
    expect(retry2.request.headers.get('Authorization')).toBe('Bearer brand-new-token');

    retry1.flush({ id: 1 });
    retry2.flush({ id: 2 });

    expect(res1).toEqual({ id: 1 });
    expect(res2).toEqual({ id: 2 });
  });
});
