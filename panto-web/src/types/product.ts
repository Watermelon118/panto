export interface ProductSummary {
  id: number;
  sku: string;
  name: string;
  category: string;
  unit: string;
  referencePurchasePrice: number;
  referenceSalePrice: number;
  safetyStock: number;
  gstApplicable: boolean;
  active: boolean;
  currentStock: number;
}

export interface Product extends ProductSummary {
  specification: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: number;
  updatedBy: number;
}

export interface ProductPage {
  items: ProductSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateProductRequest {
  sku: string;
  name: string;
  category: string;
  specification?: string;
  unit: string;
  referencePurchasePrice: number;
  referenceSalePrice: number;
  safetyStock: number;
  gstApplicable: boolean;
}

export type UpdateProductRequest = CreateProductRequest;
