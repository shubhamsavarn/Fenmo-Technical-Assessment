/**
 * Typed API client for the Expense Tracker backend.
 *
 * Design decisions:
 * 1. Returns ApiResult<T> tuple — forces explicit error handling (no silent catches)
 * 2. Automatically includes Idempotency-Key header for POST requests
 * 3. 60s timeout via AbortController (handles cloud cold starts)
 * 4. Maps HTTP errors to domain-specific ApiError types
 * 5. Never throws — all errors are returned as values
 */

import type {
  ApiResult,
  CreateExpenseRequest,
  Expense,
  ExpensePageResponse,
  CategoriesResponse,
  SortOption,
} from '../types/expense';

const BASE_URL = (import.meta.env.VITE_API_URL as string) || '/api';
const TIMEOUT_MS = 60_000;

// ── Private helpers ──────────────────────────────────────────

async function request<T>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResult<T>> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });

    clearTimeout(timeout);

    if (response.ok) {
      const data = await response.json();
      return { ok: true, data, status: response.status };
    }

    // Parse RFC 7807 problem detail
    let problem;
    try {
      problem = await response.json();
    } catch {
      problem = { detail: 'Unknown error' };
    }

    if (response.status === 400) {
      return {
        ok: false,
        error: {
          code: 'VALIDATION',
          message: problem.detail || 'Validation error',
          violations: problem.violations,
        },
      };
    }

    if (response.status === 409) {
      return {
        ok: false,
        error: { code: 'CONFLICT', message: problem.detail || 'Conflict' },
      };
    }

    if (response.status === 422) {
      return {
        ok: false,
        error: {
          code: 'VALIDATION',
          message: problem.detail || 'Invalid request',
        },
      };
    }

    return {
      ok: false,
      error: { code: 'SERVER', message: problem.detail || 'Server error' },
    };
  } catch (err) {
    clearTimeout(timeout);

    if (err instanceof DOMException && err.name === 'AbortError') {
      return {
        ok: false,
        error: { code: 'TIMEOUT', message: 'Request timed out. Please try again.' },
      };
    }

    return {
      ok: false,
      error: {
        code: 'NETWORK',
        message: 'Could not connect to server. Check your network.',
      },
    };
  }
}

// ── Public API ──────────────────────────────────────────────

export async function createExpense(
  data: CreateExpenseRequest,
  idempotencyKey: string
): Promise<ApiResult<Expense>> {
  return request<Expense>(`${BASE_URL}/expenses`, {
    method: 'POST',
    headers: {
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(data),
  });
}

export async function getExpenses(params: {
  category?: string;
  sort?: SortOption;
  page?: number;
  size?: number;
}): Promise<ApiResult<ExpensePageResponse>> {
  const searchParams = new URLSearchParams();

  if (params.category) searchParams.set('category', params.category);
  if (params.sort) searchParams.set('sort', params.sort);
  if (params.page !== undefined) searchParams.set('page', String(params.page));
  if (params.size) searchParams.set('size', String(params.size));

  const query = searchParams.toString();
  const url = `${BASE_URL}/expenses${query ? `?${query}` : ''}`;

  return request<ExpensePageResponse>(url);
}

export async function getCategories(): Promise<ApiResult<CategoriesResponse>> {
  return request<CategoriesResponse>(`${BASE_URL}/expenses/categories`);
}
