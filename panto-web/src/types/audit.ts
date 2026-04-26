export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'ROLLBACK' | 'LOGIN' | 'LOGIN_FAIL';

export interface AuditLogEntry {
  id: number;
  operatorId: number | null;
  operatorUsername: string | null;
  operatorRole: string | null;
  entityType: string;
  entityId: number | null;
  action: AuditAction;
  description: string | null;
  ipAddress: string | null;
  beforeValue: unknown;
  afterValue: unknown;
  createdAt: string;
}

export interface AuditLogPage {
  items: AuditLogEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
