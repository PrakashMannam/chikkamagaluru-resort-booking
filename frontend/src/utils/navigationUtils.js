/**
 * Navigation and layout utilities.
 */

/**
 * Determines whether the shared GuestLayout header should render the top-right
 * "Check Availability" CTA button.
 *
 * Per the reference mockup and client brief:
 * - On the Landing Page ('/'): Renders "Check Availability" (smooth-scrolls / focuses hero search).
 * - On ALL Inner Booking & Enquiry Pages ('/stay', '/stay/*', '/rooms/*', '/checkout',
 *   '/booking/*', '/payment/*', '/enquire'): DO NOT render the "Check Availability" button.
 *   The right side of the navbar on inner pages displays a clean utility link ("← Back to Estate").
 */
export function shouldShowHeaderCta(pathname) {
  if (!pathname) return false;
  // ONLY render the top-right Check Availability button on the Landing Page
  return pathname === '/';
}
