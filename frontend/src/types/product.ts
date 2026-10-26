export interface ProductResponse {
  id: number;
  name: string;
  description: string | null;
  price: number;
  stockQuantity: number;
  category: string;
  createdAt: string;
  updatedAt: string | null;
}

export interface ProductRequest {
  name: string;
  description?: string;
  price: number;
  stockQuantity: number;
  category: string;
}

export interface StockReservationItemResponse {
  productId: number;
  quantity: number;
  unitPrice?: number;
}

export interface StockReservationResponse {
  reservationId: string;
  reference: string;
  status: 'RESERVED' | 'RELEASED' | 'CONFIRMED' | string;
  items: StockReservationItemResponse[];
  createdAt: string;
  releasedAt?: string | null;
}
