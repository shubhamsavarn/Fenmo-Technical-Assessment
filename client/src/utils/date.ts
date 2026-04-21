/**
 * Date formatting utilities.
 */

/**
 * Format ISO date string to a human-readable format.
 * "2026-04-21" → "21 Apr 2026"
 */
export function formatDate(isoDate: string): string {
  const date = new Date(isoDate + 'T00:00:00'); // Prevent timezone shift
  return date.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

/**
 * Get today's date as ISO string (for form default).
 */
export function todayISO(): string {
  return new Date().toISOString().split('T')[0];
}
