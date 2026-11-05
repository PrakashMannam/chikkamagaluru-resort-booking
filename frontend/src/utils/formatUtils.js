/**
 * Formatting utilities for resort pricing and copy.
 * Strictly presents server-provided numbers/strings without calculating prices, taxes, or discounts.
 */

export function formatINR(amount) {
  if (amount === undefined || amount === null || amount === '') return '';
  const num = typeof amount === 'number' ? amount : Number(amount);
  if (Number.isNaN(num)) return String(amount);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(num);
}
