/**
 * Date utility helpers that treat dates strictly as local YYYY-MM-DD strings.
 * Never constructs with new Date("YYYY-MM-DD") to prevent UTC timezone shifts.
 */

export const BOOKING_WINDOW = {
  MIN_DATE: '2026-11-02',
  MAX_DATE: '2027-04-30',
  DEFAULT_CHECK_IN: '2026-11-10',
  DEFAULT_CHECK_OUT: '2026-11-12',
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
