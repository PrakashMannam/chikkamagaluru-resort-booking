/**
 * Navigation and layout utilities.
 */

/**
 * Determines whether the shared GuestLayout header should render the generic
 * "Check Availability" CTA.
 *
 * It is contextually suppressed on:
 * - /stay: The availability page already features a prominent page-level date/guest search control.
 * - /rooms/:code: The room details page provides an in-situ "Change dates" action adjacent to the stay criteria.
 * - /checkout, /payment, /bookings: Funnel stages where leaving the flow is discouraged.
 *
 * It remains visible on:
 * - / (Landing Page)
 * - /enquire
 * - /privacy, /terms, etc.
 */
export function shouldShowHeaderCta(pathname) {
  if (!pathname) return true;
  if (pathname === '/stay' || pathname.startsWith('/stay/')) return false;
  if (pathname.startsWith('/rooms')) return false;
  if (pathname.startsWith('/checkout')) return false;
  if (pathname.startsWith('/payment')) return false;
  if (pathname.startsWith('/bookings')) return false;
  return true;
}
