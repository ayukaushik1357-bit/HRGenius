export interface AuditLog {
  id: number;
  entityName: string;
  entityId: number;
  action: string;
  changedBy: string;
  changedAt: string;
  oldValue?: string;
  newValue?: string;
  ipAddress?: string;
}

export interface PagedAuditLog {
  content: AuditLog[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type AuditLogPage = PagedAuditLog;
