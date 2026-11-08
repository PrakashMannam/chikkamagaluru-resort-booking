import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  BOOKING_WINDOW,
  isValidStayDateRange,
  isValidFutureDate,
  parseLocalDate,
  formatLocalDate,
  getNextDay,
} from '../src/utils/dateUtils.js';
import { shouldShowHeaderCta } from '../src/utils/navigationUtils.js';

test('stay date ranges must be valid, future, ordered, and within the booking window', () => {
  assert.equal(isValidStayDateRange('2026-11-14', '2026-11-16', '2026-11-08'), true);
  assert.equal(isValidStayDateRange('2026-11-14', '2026-11-16', '2026-11-15'), false);
  assert.equal(isValidStayDateRange('bad', '2026-11-16', '2026-11-08'), false);
  assert.equal(isValidStayDateRange('2026-11-16', '2026-11-14', '2026-11-08'), false);
  assert.equal(isValidStayDateRange('2027-04-30', '2027-05-01', '2026-11-08'), false);
  assert.equal(isValidStayDateRange('2026-02-30', '2026-03-02', '2026-02-01'), false);
});

test('isValidFutureDate validates stay dates against local calendar', () => {
  const todayStr = formatLocalDate(new Date());
  assert.equal(isValidFutureDate(todayStr), true, 'Today must be allowed');

  const tomorrowStr = getNextDay(todayStr);
  assert.equal(isValidFutureDate(tomorrowStr), true, 'Tomorrow must be allowed');

  assert.equal(isValidFutureDate('2020-01-01'), false, 'Past date 2020 must be rejected');
  assert.equal(isValidFutureDate(''), false, 'Empty string must be rejected');
  assert.equal(isValidFutureDate(null), false, 'null must be rejected');
  assert.equal(isValidFutureDate('invalid-date'), false, 'Invalid format must be rejected');
});

test('GuestAvailabilityPage requires valid query dates and does not silently default or fetch without them', () => {
  const availSrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.jsx', 'utf8');

  assert.ok(availSrc.includes('isValidStayDateRange(queryCheckIn, queryCheckOut)'));
  assert.ok(!availSrc.includes('DEFAULT_CHECK_IN') && !availSrc.includes('DEFAULT_CHECK_OUT'));
  assert.ok(availSrc.includes('if (!checkIn || !checkOut)'));

  // Auto-fetches immediately in useEffect
  assert.ok(
    availSrc.includes('loadAvailability();'),
    'GuestAvailabilityPage must call loadAvailability in useEffect'
  );

  // Empty date-first state has an availability action; valid-date state has a modification action.
  assert.ok(
    availSrc.includes("{datesSelected ? 'Modify Search' : 'Check Availability'}"),
    'Stay submit button must say Check Availability before the first search and Modify Search afterwards'
  );
  assert.ok(
    !availSrc.includes('>Search Availability<') && !availSrc.includes('>Search availability<'),
    'GuestAvailabilityPage must NOT render a button labeled "Search Availability"'
  );

  // Empty state is guarded by data && !hasAvailableRooms (never renders broken "for to is")
  assert.ok(
    availSrc.includes('!isLoading && !error && data && !hasAvailableRooms'),
    'Empty state must only render when data is loaded and hasAvailableRooms is false'
  );
});

test('LandingPage: Hero buttons and search bar deduplication', () => {
  const landingSrc = fs.readFileSync('frontend/src/pages/LandingPage.jsx', 'utf8');

  // Hero Book a Stay focuses the date picker
  assert.ok(
    landingSrc.includes('Book a Stay'),
    'Hero must render "Book a Stay"'
  );
  assert.ok(
    landingSrc.includes("document.getElementById('lp-checkin')"),
    'Book a Stay action must focus the hero check-in date input'
  );

  // Hero search bar button is "Search Availability"
  assert.ok(
    landingSrc.includes('Search Availability'),
    'Hero search submit button must be "Search Availability"'
  );
});

test('Navbar route-aware CTA contract: shouldShowHeaderCta is true ONLY on Landing Page', () => {
  // Visible on landing page
  assert.equal(shouldShowHeaderCta('/'), true, 'Header CTA must be visible on landing page (/)');

  // Hidden on all inner booking and enquiry pages
  assert.equal(shouldShowHeaderCta('/stay'), false, 'Header CTA must be hidden on /stay');
  assert.equal(shouldShowHeaderCta('/stay/'), false, 'Header CTA must be hidden on /stay/');
  assert.equal(shouldShowHeaderCta('/stay/checkout'), false, 'Header CTA must be hidden on /stay/checkout');
  assert.equal(shouldShowHeaderCta('/rooms/ROOM'), false, 'Header CTA must be hidden on /rooms/:code');
  assert.equal(shouldShowHeaderCta('/rooms/VILLA'), false, 'Header CTA must be hidden on /rooms/:code');
  assert.equal(shouldShowHeaderCta('/checkout'), false, 'Header CTA must be hidden on /checkout');
  assert.equal(shouldShowHeaderCta('/payment/BKG123'), false, 'Header CTA must be hidden on /payment/:ref');
  assert.equal(shouldShowHeaderCta('/bookings/BKG123'), false, 'Header CTA must be hidden on /bookings/:ref');
  assert.equal(shouldShowHeaderCta('/enquire'), false, 'Header CTA must be hidden on /enquire');
});

test('Navbar renders Back to Estate utility link on inner pages instead of Check Availability', () => {
  const layoutSrc = fs.readFileSync('frontend/src/components/layout/GuestLayout.jsx', 'utf8');

  // Checks shouldShowHeaderCta
  assert.ok(
    layoutSrc.includes('const showHeaderCta = shouldShowHeaderCta(location.pathname);'),
    'GuestLayout must determine CTA visibility via shouldShowHeaderCta'
  );

  // Renders "← Back to Estate" when showHeaderCta is false
  assert.ok(
    layoutSrc.includes('&larr; Back to Estate') || layoutSrc.includes('Back to Estate'),
    'GuestLayout must render "Back to Estate" utility link on inner pages'
  );
});

test('Guest navigation includes an accessible mobile menu and 44px touch targets', () => {
  const layoutSrc = fs.readFileSync('frontend/src/components/layout/GuestLayout.jsx', 'utf8');
  const cssSrc = fs.readFileSync('frontend/src/components/layout/GuestLayout.css', 'utf8');
  assert.ok(layoutSrc.includes('aria-expanded={mobileMenuOpen}'));
  assert.ok(layoutSrc.includes('aria-controls="guest-main-navigation"'));
  assert.ok(cssSrc.includes('.guest-header__nav.is-open'));
  assert.ok(cssSrc.includes('min-height: var(--tap-min)'));
  assert.ok(cssSrc.includes('@media (max-width: 480px)'));
  assert.ok(/\.guest-header__cta\s*\{\s*display:\s*none;/.test(cssSrc));
});

test('Landing guest selector has an explicit programmatic label', () => {
  const landingSrc = fs.readFileSync('frontend/src/pages/LandingPage.jsx', 'utf8');
  assert.ok(landingSrc.includes('htmlFor="lp-search-guests"'));
  assert.ok(landingSrc.includes('id="lp-search-guests"'));
});

test('Staff inventory dates use current IST time instead of stale demo dates', () => {
  const staffSrc = fs.readFileSync('frontend/src/pages/staff/StaffInventoryPage.jsx', 'utf8');
  assert.ok(staffSrc.includes('const DEFAULT_FROM = getTodayIst();'));
  assert.ok(staffSrc.includes('const DEFAULT_TO = addIsoDays(DEFAULT_FROM, 13);'));
  assert.ok(staffSrc.includes('One week ahead'));
  assert.ok(!staffSrc.includes('2026-11-05') && !staffSrc.includes('2026-11-14'));
});

test('RoomDetailPage: Preserves stay criteria, provides "Change dates", and primary button is "Book Now"', () => {
  const roomSrc = fs.readFileSync('frontend/src/pages/RoomDetailPage.jsx', 'utf8');

  // Preserved stay criteria strip
  assert.ok(
    roomSrc.includes('room-detail__stay-criteria'),
    'RoomDetailPage must render stay criteria container'
  );

  // Change dates action
  assert.ok(
    roomSrc.includes('room-detail__change-dates-btn'),
    'RoomDetailPage must render Change dates button'
  );
  assert.ok(
    roomSrc.includes('Change dates'),
    'Action button text must be "Change dates"'
  );

  // Primary rate plan action is "Book Now"
  assert.ok(
    roomSrc.includes('>Book Now<') || roomSrc.includes('Book Now\n') || roomSrc.includes('Book Now\r\n'),
    'Rate plan action button must be labeled "Book Now"'
  );
});

test('GuestCheckoutPage: Primary action button is "Proceed to Payment"', () => {
  const checkoutSrc = fs.readFileSync('frontend/src/pages/GuestCheckoutPage.jsx', 'utf8');
  assert.ok(
    checkoutSrc.includes('Proceed to Payment'),
    'GuestCheckoutPage primary button must be "Proceed to Payment"'
  );
});

test('PublicEnquiryPage: Single primary action is "Submit Enquiry"', () => {
  const enquireSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');
  assert.ok(
    enquireSrc.includes('Submit Enquiry'),
    'PublicEnquiryPage primary button must be "Submit Enquiry"'
  );
  // Must NOT render availability search bar or buttons
  assert.ok(
    !enquireSrc.includes('Search Availability') && !enquireSrc.includes('Check Availability'),
    'PublicEnquiryPage must NOT render Check Availability or Search Availability buttons'
  );
});

test('Single Primary CTA Audit: No page renders both "Check Availability" and "Search Availability"', () => {
  const layoutSrc = fs.readFileSync('frontend/src/components/layout/GuestLayout.jsx', 'utf8');
  const availSrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.jsx', 'utf8');
  const roomSrc = fs.readFileSync('frontend/src/pages/RoomDetailPage.jsx', 'utf8');
  const checkoutSrc = fs.readFileSync('frontend/src/pages/GuestCheckoutPage.jsx', 'utf8');
  const enquireSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // On /stay, /rooms, /checkout, /enquire: header CTA is suppressed by shouldShowHeaderCta
  assert.equal(shouldShowHeaderCta('/stay'), false);
  assert.equal(shouldShowHeaderCta('/rooms/ROOM'), false);
  assert.equal(shouldShowHeaderCta('/checkout'), false);
  assert.equal(shouldShowHeaderCta('/enquire'), false);

  // On /stay: the button label is state-specific, so the no-date form is actionable.
  assert.ok(availSrc.includes("{datesSelected ? 'Modify Search' : 'Check Availability'}"));
  assert.ok(!availSrc.includes('>Search Availability<'));

  // On room details: primary button is "Book Now"
  assert.ok(!roomSrc.includes('>Check Availability<'));
  assert.ok(!roomSrc.includes('>Search Availability<'));

  // On checkout: primary button is "Proceed to Payment"
  assert.ok(!checkoutSrc.includes('>Check Availability<'));
  assert.ok(!checkoutSrc.includes('>Search Availability<'));

  // On enquire: primary button is "Submit Enquiry"
  assert.ok(!enquireSrc.includes('>Check Availability<'));
  assert.ok(!enquireSrc.includes('>Search Availability<'));
});
