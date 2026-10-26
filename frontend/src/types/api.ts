export interface SpringSort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface SpringPageable {
  offset: number;
  pageNumber: number;
  pageSize: number;
  paged: boolean;
  unpaged: boolean;
  sort: SpringSort;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // 0-indexed
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  sort?: SpringSort;
  pageable?: SpringPageable;
}

export interface ApiErrorResponse {
  error?: string;
  message?: string;
  messages?: Record<string, string>; // Validation errors map
  status?: number;
  timestamp?: string;
  path?: string;
}

export interface ParsedApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
  isGatewayUnavailable?: boolean;
}
