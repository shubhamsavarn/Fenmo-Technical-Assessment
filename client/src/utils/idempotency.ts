/**
 * Idempotency key management using localStorage.
 *
 * Lifecycle:
 * - Form opened → generate new UUID, store in localStorage
 * - Submit succeeds (201/200) → CLEAR (next submit is a new operation)
 * - Validation error (400) → CLEAR (key was wasted, need fresh one)
 * - Network error / timeout → KEEP (request may have been processed server-side)
 * - Server error (5xx) → KEEP (might have partially processed)
 * - Page refresh → key SURVIVES (localStorage persists)
 */

const STORAGE_PREFIX = 'idem_';

export function getOrCreateIdempotencyKey(formId: string): string {
  const storageKey = STORAGE_PREFIX + formId;
  let key = localStorage.getItem(storageKey);
  if (!key) {
    key = crypto.randomUUID();
    localStorage.setItem(storageKey, key);
  }
  return key;
}

export function clearIdempotencyKey(formId: string): void {
  localStorage.removeItem(STORAGE_PREFIX + formId);
}

export function refreshIdempotencyKey(formId: string): string {
  clearIdempotencyKey(formId);
  return getOrCreateIdempotencyKey(formId);
}
