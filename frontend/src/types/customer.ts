export interface CustomerResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  company: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface CustomerRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  company?: string;
}
