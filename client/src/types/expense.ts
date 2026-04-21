/**
 * TypeScript interfaces mirroring the API contract.
 * Amount is always a string — never a number — to prevent float precision issues.
 */

export interface Expense {
  id: string;
  amount: string;        // String to avoid JS float precision issues
  category: string;
  description: string;
  date: string;          // ISO date "2026-04-21"
  createdAt: string;     // ISO datetime
}

export interface CreateExpenseRequest {
  amount: string;
  category: string;
  description: string;
  date: string;
}

export interface PageMeta {
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SummaryMeta {
  totalAmount: string;
  count: number;
  currency: string;
}

export interface ExpensePageResponse {
  content: Expense[];
  page: PageMeta;
  summary: SummaryMeta;
}

export interface CategoriesResponse {
  categories: string[];
}

/** RFC 7807 Problem Detail */
export interface ProblemDetail {
  type?: string;
  title: string;
  status: number;
  detail: string;
  violations?: Array<{ field: string; message: string }>;
}

/** Result type — forces explicit error handling */
export type ApiResult<T> =
  | { ok: true; data: T; status: number }
  | { ok: false; error: ApiError };

export interface ApiError {
  code: 'VALIDATION' | 'CONFLICT' | 'NETWORK' | 'TIMEOUT' | 'SERVER';
  message: string;
  violations?: Array<{ field: string; message: string }>;
}

/** Form submit state machine */
export type FormState = 'IDLE' | 'SUBMITTING' | 'SUCCESS' | 'ERROR';

/** Sort options */
export type SortOption = 'date_desc' | 'date_asc' | 'amount_desc' | 'amount_asc';
