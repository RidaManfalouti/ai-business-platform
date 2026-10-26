export type SaleStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface SaleItemRequest {
  productId: number;
  quantity: number;
}

export interface CreateSaleRequest {
  customerId: number;
  reference?: string;
  items: SaleItemRequest[];
}

export interface SaleItemResponse {
  id: number;
  productId: number;
  productNameSnapshot: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface SaleResponse {
  id: number;
  reference: string;
  customerId: number;
  status: SaleStatus;
  totalAmount: number;
  items: SaleItemResponse[];
  createdAt: string;
  updatedAt: string | null;
}

export interface UpdateSaleStatusRequest {
  status: SaleStatus;
}
