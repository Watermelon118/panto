import type { ExpiryStatus } from './inventory';

export interface DestructionSummary {
  id: number;
  destructionNumber: string;
  batchId: number;
  batchNumber: string;
  batchExpiryDate: string;
  batchExpiryStatus: ExpiryStatus;
  productId: number;
  productSku: string;
  productName: string;
  quantityDestroyed: number;
  purchaseUnitPriceSnapshot: number;
  lossAmount: number;
  reason: string;
  createdAt: string;
  createdBy: number;
}

export interface DestructionDetail extends DestructionSummary {
  batchQuantityRemaining: number;
  inventoryTransactionId: number;
}

export interface DestructionPage {
  items: DestructionSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateDestructionRequest {
  batchId: number;
  quantityDestroyed: number;
  reason: string;
}
