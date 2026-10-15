export interface TemplateTask {
  id?: number;
  title: string;
  description?: string;
  category: 'DOCUMENTS' | 'IT_SETUP' | 'INDUCTION' | 'TRAINING';
  defaultDaysToComplete: number;
}

export interface OnboardingTemplate {
  id: number;
  name: string;
  description?: string;
  tasks: TemplateTask[];
}

export interface OnboardingTask {
  id: number;
  employeeId: number;
  employeeName: string;
  title: string;
  description?: string;
  category: 'DOCUMENTS' | 'IT_SETUP' | 'INDUCTION' | 'TRAINING';
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  ownerId?: number;
  ownerName?: string;
  dueDate?: string;
  completedAt?: string;
}

export interface OnboardingProgress {
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  totalTasks: number;
  completedTasks: number;
  progressPercentage: number;
  tasks: OnboardingTask[];
}
