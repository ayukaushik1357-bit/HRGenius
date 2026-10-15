export interface JobPosting {
  id: number;
  title: string;
  description?: string;
  requirements?: string;
  location?: string;
  minExperienceYears?: number;
  maxExperienceYears?: number;
  openings: number;
  status: 'DRAFT' | 'OPEN' | 'CLOSED';
  departmentId?: number;
  departmentName?: string;
  designationId?: number;
  designationTitle?: string;
  createdBy?: string;
  createdAt?: string;
}

export interface Candidate {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  resumeFileName?: string;
  resumeFilePath?: string;
  experienceYears?: number;
  currentCompany?: string;
  expectedSalaryCTC?: number;
  createdAt?: string;
}

export interface Application {
  id: number;
  jobPostingId: number;
  jobPostingTitle: string;
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  candidatePhone?: string;
  resumeFileName?: string;
  stage: 'APPLIED' | 'SCREENING' | 'INTERVIEW' | 'OFFERED' | 'HIRED' | 'REJECTED';
  appliedAt: string;
  notes?: string;
}

export interface Interview {
  id?: number;
  applicationId: number;
  candidateId?: number;
  candidateName?: string;
  jobPostingTitle?: string;
  interviewerId: number;
  interviewerName?: string;
  scheduledAt: string;
  roundName: string;
  rating?: number;
  feedback?: string;
  status?: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
}

export interface Offer {
  id?: number;
  applicationId: number;
  candidateId?: number;
  candidateName?: string;
  jobPostingTitle?: string;
  offerDate: string;
  joiningDate: string;
  offeredSalaryCTC: number;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  remarks?: string;
}

export interface HireCandidateRequest {
  applicationId: number;
  employeeCode: string;
  departmentId: number;
  designationId: number;
  managerId?: number;
  joiningDate: string;
  officialEmail: string;
  initialPassword: string;
  onboardingTemplateId?: number;
  basicSalary?: number;
  hra?: number;
  specialAllowance?: number;
  otherAllowances?: number;
}
