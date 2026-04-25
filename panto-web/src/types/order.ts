export type OrderStatus = 'ACTIVE' | 'ROLLED_BACK';

export interface OrderSummary {
  id: number;
  orderNumber: string;
  customerId: number;
  customerCompanyName: string;
  status: OrderStatus;
  itemCount: number;
  subtotalAmount: number;
  gstAmount: number;
  totalAmount: number;
  createdAt: string;
  createdBy: number;
}

export interface OrderItem {
  id: number;
  productId: number;
  batchId: number;
  batchNumber: string | null;
  batchExpiryDate: string | null;
  batchExpiryStatus: string | null;
  productSku: string;
  productName: string;
  productUnit: string;
  productSpecification: string | null;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  gstApplicable: boolean;
  gstAmount: number;
}

export interface OrderDetail {
  id: number;
  orderNumber: string;
  customerId: number;
  customerCompanyName: string;
  status: OrderStatus;
  subtotalAmount: number;
  gstAmount: number;
  totalAmount: number;
  remarks: string | null;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
  createdBy: number;
  updatedBy: number;
}

export interface OrderPage {
  items: OrderSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateOrderItemRequest {
  productId: number;
  quantity: number;
  unitPrice?: number;
}

export interface CreateOrderRequest {
  customerId: number;
  remarks?: string;
  items: CreateOrderItemRequest[];
}
