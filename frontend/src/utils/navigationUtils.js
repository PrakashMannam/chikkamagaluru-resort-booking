/**
 * Navigation and layout utilities.
 */

/**
 * Determines whether the shared GuestLayout header should render the top-right
 * "Check Availability" CTA button.
 *
 * Per the reference mockup and client brief:
 * - On the Landing Page ('/'), Weddings ('/weddings'), and Offsites ('/offsites'):
 *   Renders "Check Availability" (smooth-scrolls / focuses hero search on '/',
 *   or navigates directly to '/stay' from dedicated event pages).
 * - On /stay and all inner booking routes ('/stay', '/stay/*', '/rooms/*', '/checkout',
 *   '/booking/*', '/payment/*', '/enquire'): DO NOT render the "Check Availability" button,
 *   preventing duplicate "Check Availability" controls on screen.
 *   The right side of the navbar renders a clean utility link ("← Back to Estate").
 */
export function shouldShowHeaderCta(pathname) {
  if (!pathname) return false;
  if (
    pathname === '/stay' ||
    pathname.startsWith('/stay/') ||
    pathname.startsWith('/rooms') ||
    pathname.startsWith('/checkout') ||
    pathname.startsWith('/payment') ||
    pathname.startsWith('/bookings') ||
    pathname === '/enquire'
  ) {
    return false;
  }
  return pathname === '/' || pathname === '/weddings' || pathname === '/offsites';
}
