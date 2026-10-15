export interface Address {
  id?: number;
  addressType: 'PERMANENT' | 'CURRENT';
  line1: string;
  line2?: string;
  city: string;
  state: string;
  country: string;
  zipCode: string;
}

export interface EmergencyContact {
  id?: number;
  name: string;
  relationship: string;
  phone: string;
}

export interface DocumentMetadata {
  id: number;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  documentType: 'ID_PROOF' | 'OFFER_LETTER' | 'RESUME' | 'OTHER';
  uploadedAt: string;
}

export interface Employee {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  joiningDate: string;
  departmentId?: number;
  departmentName?: string;
  designationId?: number;
  designationTitle?: string;
  reportingManagerId?: number;
  reportingManagerName?: string;
  employmentType: 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERN';
  status: 'ACTIVE' | 'ON_NOTICE' | 'EXITED';
  salaryCTC?: number;
  panNumber?: string;
  aadhaarNumber?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankName?: string;
  addresses?: Address[];
  emergencyContacts?: EmergencyContact[];
  documents?: DocumentMetadata[];
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface EmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  joiningDate: string;
  departmentId?: number;
  designationId?: number;
  reportingManagerId?: number;
  employmentType: 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERN';
  status: 'ACTIVE' | 'ON_NOTICE' | 'EXITED';
  salaryCTC?: number;
  panNumber?: string;
  aadhaarNumber?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankName?: string;
  addresses?: Address[];
  emergencyContacts?: EmergencyContact[];
}
