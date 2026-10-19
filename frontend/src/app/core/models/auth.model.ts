export interface LoginRequest {
  username: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  username: string;
  email: string;
  roles: string[];
  mustChangePassword?: boolean;
}

export interface CurrentUser {
  username: string;
  email?: string;
  roles: string[];
  mustChangePassword?: boolean;
}

export interface AuthConfig {
  signupEnabled: boolean;
  googleEnabled: boolean;
  googleClientId?: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface GoogleLoginResponse {
  status: 'SIGNED_IN' | 'PENDING_APPROVAL';
  message?: string;
  auth?: AuthResponse;
}
