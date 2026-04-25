export type ExpiryStatus = 'NORMAL' | 'EXPIRING_SOON' | 'EXPIRED';
export type TransactionType = 'IN' | 'OUT' | 'ROLLBACK' | 'DESTROY' | 'ADJUST';

export interface StockSummary {
  productId: number;
  sku: string;
  name: string;
  category: string;
  unit: string;
  safetyStock: number;
  currentStock: number;
  belowSafetyStock: boolean;
}

export interface StockPage {
  items: StockSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BatchItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  batchNumber: string;
  arrivalDate: string;
  expiryDate: string;
  quantityReceived: number;
  quantityRemaining: number;
  purchaseUnitPrice: number;
  expiryStatus: ExpiryStatus;
  createdAt: string;
}

export interface BatchPage {
  items: BatchItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface InventoryTransaction {
  id: number;
  batchId: number;
  batchNumber: string;
  productId: number;
  productSku: string;
  productName: string;
  transactionType: TransactionType;
  quantityDelta: number;
  quantityBefore: number;
  quantityAfter: number;
  relatedDocumentType: string | null;
  relatedDocumentId: number | null;
  note: string | null;
  createdAt: string;
  createdBy: number;
}

export interface TransactionPage {
  items: InventoryTransaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
