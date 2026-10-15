export interface Designation {
  id?: number;
  title: string;
  level?: number;
  description?: string;
  createdBy?: string;
  createdAt?: string;
}

export interface DesignationRequest {
  title: string;
  level?: number;
  description?: string;
}
