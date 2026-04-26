import type { UserRole } from './auth';

export interface DashboardWarnings {
  lowStockCount: number;
  expiringSoonCount: number;
  expiredCount: number;
}

export interface TopProductSummary {
  productId: number;
  productSku: string;
  productName: string;
  quantitySold: number;
  salesAmount: number;
}

export interface ManagerSummary {
  todaySalesTotal: number;
  monthSalesTotal: number;
  pendingTaskCount: number;
  topProducts: TopProductSummary[];
}

export interface WarehouseSummary {
  todayInboundCount: number;
  todayOutboundCount: number;
  pendingDestructionCount: number;
}

export interface AccountantSummary {
  monthSalesTotal: number;
  monthLossTotal: number;
}

export interface DashboardSummary {
  role: UserRole;
  warnings: DashboardWarnings;
  managerSummary: ManagerSummary | null;
  warehouseSummary: WarehouseSummary | null;
  accountantSummary: AccountantSummary | null;
}
