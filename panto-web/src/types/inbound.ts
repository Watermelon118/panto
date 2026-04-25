export interface InboundItemResponse {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  batchNumber: string;
  expiryDate: string;
  quantity: number;
  purchaseUnitPrice: number;
  remarks: string | null;
}

export interface InboundSummary {
  id: number;
  inboundNumber: string;
  inboundDate: string;
  itemCount: number;
  remarks: string | null;
  createdAt: string;
  createdBy: number;
}

export interface InboundDetail extends Omit<InboundSummary, 'itemCount'> {
  items: InboundItemResponse[];
  updatedAt: string;
  updatedBy: number;
}

export interface InboundPage {
  items: InboundSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface InboundItemRequest {
  productId: number;
  expiryDate: string;
  quantity: number;
  purchaseUnitPrice: number;
  remarks?: string;
}

export interface CreateInboundRequest {
  inboundDate: string;
  remarks?: string;
  items: InboundItemRequest[];
}

export type UpdateInboundRequest = CreateInboundRequest;
