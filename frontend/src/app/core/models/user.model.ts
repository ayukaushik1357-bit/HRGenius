export interface UserResponse {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  mustChangePassword: boolean;
  failedAttempts: number;
  lockoutTime?: string;
  locked: boolean;
  roles: string[];
  employeeId?: number;
  employeeCode?: string;
  employeeName?: string;
  createdAt?: string;
}

export interface UserStatusRequest {
  enabled: boolean;
}

export interface UserRolesRequest {
  roles: string[];
}
