export interface Department {
  id?: number;
  name: string;
  code: string;
  description?: string;
  createdBy?: string;
  createdAt?: string;
}

export interface DepartmentRequest {
  name: string;
  code: string;
  description?: string;
}
