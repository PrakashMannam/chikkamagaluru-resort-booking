/**
 * Date utility helpers that treat dates strictly as local YYYY-MM-DD strings.
 * Never constructs with new Date("YYYY-MM-DD") to prevent UTC timezone shifts.
 */

export const BOOKING_WINDOW = {
  MIN_DATE: '2026-11-02',
  MAX_DATE: '2027-04-30',
  // No static DEFAULT_CHECK_IN / DEFAULT_CHECK_OUT — guests must select dates.
};

export function parseLocalDate(dateStr) {
  if (!dateStr || typeof dateStr !== 'string') return null;
  const parts = dateStr.split('-');
  if (parts.length !== 3) return null;
  const [year, month, day] = parts.map(Number);
  if (Number.isNaN(year) || Number.isNaN(month) || Number.isNaN(day)) return null;
  return new Date(year, month - 1, day);
}

export function formatLocalDate(date) {
  if (!date || !(date instanceof Date) || Number.isNaN(date.getTime())) return '';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function calculateNights(checkInStr, checkOutStr) {
  const checkIn = parseLocalDate(checkInStr);
  const checkOut = parseLocalDate(checkOutStr);
  if (!checkIn || !checkOut) return 1;
  const diffTime = checkOut.getTime() - checkIn.getTime();
  const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));
  return diffDays > 0 ? diffDays : 1;
}

export function formatDisplayDate(dateStr) {
  const d = parseLocalDate(dateStr);
  if (!d) return dateStr || '';
  return d.toLocaleDateString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function formatCompactDate(dateStr) {
  const d = parseLocalDate(dateStr);
  if (!d) return dateStr || '';
  return d.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function getNextDay(dateStr) {
  const d = parseLocalDate(dateStr);
  if (!d) return '';
  d.setDate(d.getDate() + 1);
  return formatLocalDate(d);
}

/**
 * Returns true if the YYYY-MM-DD string is a valid date that is not in the past
 * relative to today's local date.
 */
export function isValidFutureDate(dateStr) {
  if (!dateStr || !/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) return false;
  const d = parseLocalDate(dateStr);
  if (!d || Number.isNaN(d.getTime())) return false;
  const today = parseLocalDate(formatLocalDate(new Date()));
  return d >= today;
}

/**
 * Safely parses a server-provided expiry timestamp into UTC epoch milliseconds.
 * - If timestamp is numeric (ms or sec), converts directly.
 * - If string contains an explicit offset or 'Z', uses standard Date.parse.
 * - If string is timezone-less (e.g. "2026-11-08T04:15:30"), forces UTC interpretation ('Z')
 *   to avoid browser local-timezone corruption (e.g. Asia/Kolkata +05:30 shift).
 * - Returns null for missing, malformed, or invalid dates.
 */
export function parseServerExpiry(expiresAt) {
  if (expiresAt === null || expiresAt === undefined || expiresAt === '') {
    return null;
  }
  if (typeof expiresAt === 'number') {
    if (Number.isNaN(expiresAt) || !Number.isFinite(expiresAt) || expiresAt <= 0) return null;
    return expiresAt > 1e11 ? expiresAt : Math.round(expiresAt * 1000);
  }
  if (typeof expiresAt !== 'string') {
    return null;
  }
  const trimmed = expiresAt.trim();
  if (!trimmed) return null;

  // Check if string has explicit timezone ('Z', '+HH:mm', '-HH:mm', '+HHmm', etc.)
  if (/Z|[+-]\d{2}(?::?\d{2})?$/i.test(trimmed)) {
    const t = new Date(trimmed).getTime();
    return Number.isNaN(t) ? null : t;
  }

  // Timezone-less string (e.g. "2026-11-08T10:15:30" or "2026-11-08 10:15:30")
  // Force UTC parsing by appending 'Z', never let the browser parse as local time!
  const isoUtc = trimmed.includes('T') ? `${trimmed}Z` : `${trimmed.replace(' ', 'T')}Z`;
  const t = new Date(isoUtc).getTime();
  return Number.isNaN(t) ? null : t;
}
