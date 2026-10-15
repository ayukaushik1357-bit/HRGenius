export interface AppNotification {
  id: number;
  title: string;
  message: string;
  type: 'LEAVE' | 'INTERVIEW' | 'PAYROLL' | 'ONBOARDING' | 'GENERAL';
  read: boolean;
  readAt?: string;
  linkUrl?: string;
  createdAt: string;
}
