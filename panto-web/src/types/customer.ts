export interface CustomerSummary {
  id: number;
  companyName: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  active: boolean;
}

export interface Customer extends CustomerSummary {
  address: string | null;
  gstNumber: string | null;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: number;
  updatedBy: number;
}

export interface CustomerPage {
  items: CustomerSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateCustomerRequest {
  companyName: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  address?: string;
  gstNumber?: string;
  remarks?: string;
}

export type UpdateCustomerRequest = CreateCustomerRequest;
