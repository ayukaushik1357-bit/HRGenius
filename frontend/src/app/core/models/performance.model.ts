export interface ReviewCycle {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'CLOSED';
}

export interface Goal {
  id: number;
  employeeId: number;
  reviewCycleId: number;
  title: string;
  description?: string;
  weightage: number;
  progressPercentage: number;
}

export interface GoalRequest {
  employeeId?: number;
  reviewCycleId: number;
  title: string;
  description?: string;
  weightage: number;
  progressPercentage?: number;
}

export interface PeerFeedback {
  id: number;
  reviewerId: number;
  reviewerName: string;
  feedback: string;
  rating?: number;
  submittedAt: string;
}

export interface PerformanceReview {
  id: number;
  reviewCycleId: number;
  reviewCycleName: string;
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  departmentName: string;
  designationTitle: string;
  managerId?: number;
  managerName?: string;
  status: 'NOT_STARTED' | 'SELF_REVIEW' | 'MANAGER_REVIEW' | 'COMPLETED';
  selfRating?: number;
  selfComments?: string;
  managerRating?: number;
  managerComments?: string;
  finalRating?: number;
  submittedAt?: string;
  reviewedAt?: string;
  peerFeedbacks?: PeerFeedback[];
}
