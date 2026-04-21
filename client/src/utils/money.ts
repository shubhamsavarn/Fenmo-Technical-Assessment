/**
 * Money formatting utilities.
 *
 * CRITICAL RULE: This module NEVER parses money strings to floats for arithmetic.
 * All money arithmetic happens server-side with BigDecimal/paise.
 * This module only FORMATS strings for display.
 */

/**
 * Format a money string from the API for display with INR symbol and Indian grouping.
 * Input:  "1250.50" (always a string from API)
 * Output: "₹1,250.50" (with Indian number grouping for lakhs/crores)
 */
export function formatMoney(amount: string): string {
  if (!amount || amount === '0.00') return '₹0.00';

  // Number() conversion is ONLY for Intl formatting — never used for arithmetic
  const num = Number(amount);
  if (isNaN(num)) return `₹${amount}`;

  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(num);
}
