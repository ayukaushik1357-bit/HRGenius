import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthResponse } from '../models/auth.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    routerSpy = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should store tokens in memory and not in sessionStorage', () => {
    const setItemSpy = vi.spyOn(sessionStorage, 'setItem');
    const getItemSpy = vi.spyOn(sessionStorage, 'getItem');

    expect(service.getAccessToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);

    const mockResponse: AuthResponse = {
      accessToken: 'memory-only-token',
      tokenType: 'Bearer',
      username: 'john',
      email: 'john@example.com',
      roles: ['ROLE_EMPLOYEE']
    };

    service.login({ username: 'john', password: 'password' }).subscribe(res => {
      expect(res.accessToken).toBe('memory-only-token');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush(mockResponse);

    expect(service.getAccessToken()).toBe('memory-only-token');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()?.username).toBe('john');

    // Verify sessionStorage was NEVER touched
    expect(setItemSpy).not.toHaveBeenCalled();
  });

  it('should call /auth/refresh with withCredentials on initAuth', async () => {
    const mockResponse: AuthResponse = {
      accessToken: 'refreshed-token',
      tokenType: 'Bearer',
      username: 'john',
      email: 'john@example.com',
      roles: ['ROLE_EMPLOYEE']
    };

    const initPromise = service.initAuth();

    const req = httpMock.expectOne('http://localhost:8080/api/auth/refresh');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush(mockResponse);

    await initPromise;

    expect(service.getAccessToken()).toBe('refreshed-token');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.isInitialized()).toBe(true);
  });
});
