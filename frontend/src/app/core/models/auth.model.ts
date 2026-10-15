export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  username: string;
  email: string;
  roles: string[];
}

export interface CurrentUser {
  username: string;
  email?: string;
  roles: string[];
}
