import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { BOOKING_WINDOW, isValidFutureDate, parseLocalDate, formatLocalDate, getNextDay } from '../src/utils/dateUtils.js';
import { shouldShowHeaderCta } from '../src/utils/navigationUtils.js';

test('Issue 2 Regression: dateUtils does NOT contain static DEFAULT_CHECK_IN or DEFAULT_CHECK_OUT', () => {
  assert.equal(
    BOOKING_WINDOW.DEFAULT_CHECK_IN,
    undefined,
    'BOOKING_WINDOW must NOT expose a hard-coded DEFAULT_CHECK_IN'
  );
  assert.equal(
    BOOKING_WINDOW.DEFAULT_CHECK_OUT,
    undefined,
    'BOOKING_WINDOW must NOT expose a hard-coded DEFAULT_CHECK_OUT'
  );
});

test('Issue 2 Regression: isValidFutureDate validates stay dates against local calendar', () => {
  const todayStr = formatLocalDate(new Date());
  assert.equal(isValidFutureDate(todayStr), true, 'Today must be allowed');

  const tomorrowStr = getNextDay(todayStr);
  assert.equal(isValidFutureDate(tomorrowStr), true, 'Tomorrow must be allowed');

  // Hard-coded past dates from previous buggy defaults
  assert.equal(isValidFutureDate('2020-01-01'), false, 'Past date 2020 must be rejected');
  assert.equal(isValidFutureDate(''), false, 'Empty string must be rejected');
  assert.equal(isValidFutureDate(null), false, 'null must be rejected');
  assert.equal(isValidFutureDate('invalid-date'), false, 'Invalid format must be rejected');
});

test('Source Contract: GuestAvailabilityPage does not silently default stay dates and shows date-selection-first prompt', () => {
  const availSrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.jsx', 'utf8');

  // Must not have fallback to DEFAULT_CHECK_IN or DEFAULT_CHECK_OUT
  assert.ok(
    !availSrc.includes('DEFAULT_CHECK_IN'),
    'GuestAvailabilityPage must not reference DEFAULT_CHECK_IN'
  );
  assert.ok(
    !availSrc.includes('DEFAULT_CHECK_OUT'),
    'GuestAvailabilityPage must not reference DEFAULT_CHECK_OUT'
  );

  // Must define datesSelected based on checkIn && checkOut
  assert.ok(
    availSrc.includes('const datesSelected = Boolean(checkIn && checkOut);'),
    'GuestAvailabilityPage must compute datesSelected from explicit query params'
  );

  // loadAvailability must guard against missing dates and not trigger an API fetch
  assert.ok(
    availSrc.includes('if (!checkIn || !checkOut)'),
    'loadAvailability must guard against absent dates'
  );

  // Must render date-selection-first prompt when !datesSelected
  assert.ok(
    availSrc.includes('stay-date-prompt'),
    'GuestAvailabilityPage must render .stay-date-prompt when dates are not selected'
  );

  // Form submit button must show "Search availability"
  assert.ok(
    availSrc.includes('Search availability'),
    'Search form CTA must be "Search availability"'
  );
});

test('Source Contract: GuestCheckoutPage and RoomDetailPage guard against absent dates and redirect to /stay', () => {
  const checkoutSrc = fs.readFileSync('frontend/src/pages/GuestCheckoutPage.jsx', 'utf8');
  assert.ok(
    !checkoutSrc.includes('DEFAULT_CHECK_IN'),
    'GuestCheckoutPage must not reference DEFAULT_CHECK_IN'
  );
  assert.ok(
    checkoutSrc.includes("navigate('/stay', { replace: true });"),
    'GuestCheckoutPage must redirect to /stay if dates are absent'
  );

  const roomSrc = fs.readFileSync('frontend/src/pages/RoomDetailPage.jsx', 'utf8');
  assert.ok(
    !roomSrc.includes('DEFAULT_CHECK_IN'),
    'RoomDetailPage must not reference DEFAULT_CHECK_IN'
  );
  assert.ok(
    roomSrc.includes("navigate('/stay', { replace: true });"),
    'RoomDetailPage must redirect to /stay if dates are absent'
  );
});

test('Source Contract: LandingPage "Book a Stay" CTA navigates to /stay without hardcoded date params', () => {
  const landingSrc = fs.readFileSync('frontend/src/pages/LandingPage.jsx', 'utf8');

  // Hero button
  assert.ok(
    landingSrc.includes('<Link to="/stay" className="lp-btn-book">'),
    'Hero Book a Stay button must link cleanly to /stay'
  );

  // Floating search bar handler requires valid check-in and check-out before navigating
  const handleSearchIdx = landingSrc.indexOf('function handleSearch(');
  assert.ok(handleSearchIdx !== -1, 'LandingPage must contain handleSearch function');
  const searchBody = landingSrc.substring(handleSearchIdx, handleSearchIdx + 600);

  assert.ok(
    searchBody.includes("setError('Please select an arrival date.');"),
    'handleSearch must validate presence of check-in'
  );
  assert.ok(
    searchBody.includes("setError('Please select a departure date.');"),
    'handleSearch must validate presence of check-out'
  );
  assert.ok(
    searchBody.includes("setError('Arrival date cannot be in the past.');"),
    'handleSearch must validate arrival is not in the past'
  );
  assert.ok(
    searchBody.includes("setError('Departure date must follow arrival date.');"),
    'handleSearch must validate departure is after arrival'
  );
});

// ==============================================================================
// ISSUE 3 TESTS: Contextual header CTA & unambiguous availability actions
// ==============================================================================

test('Issue 3: shouldShowHeaderCta contextually hides header CTA on /stay and /rooms/:code', () => {
  // Must hide on availability results and rooms
  assert.equal(shouldShowHeaderCta('/stay'), false, 'Header CTA must be hidden on /stay');
  assert.equal(shouldShowHeaderCta('/stay/'), false, 'Header CTA must be hidden on /stay/');
  assert.equal(shouldShowHeaderCta('/rooms/ROOM'), false, 'Header CTA must be hidden on /rooms/ROOM');
  assert.equal(shouldShowHeaderCta('/rooms/VILLA'), false, 'Header CTA must be hidden on /rooms/VILLA');

  // Must hide during the 3-step booking funnel to avoid competing actions
  assert.equal(shouldShowHeaderCta('/checkout'), false, 'Header CTA must be hidden on /checkout');
  assert.equal(shouldShowHeaderCta('/payment/BKG123'), false, 'Header CTA must be hidden on /payment/:ref');
  assert.equal(shouldShowHeaderCta('/bookings/BKG123'), false, 'Header CTA must be hidden on /bookings/:ref');

  // Must remain visible on other public pages where jumping to availability is helpful
  assert.equal(shouldShowHeaderCta('/'), true, 'Header CTA must be visible on landing page');
  assert.equal(shouldShowHeaderCta('/enquire'), true, 'Header CTA must be visible on /enquire');
  assert.equal(shouldShowHeaderCta('/privacy'), true, 'Header CTA must be visible on /privacy');
  assert.equal(shouldShowHeaderCta('/terms'), true, 'Header CTA must be visible on /terms');
});

test('Issue 3: RoomDetailPage provides visible stay criteria and single clear "Change dates" action returning to /stay with preserved params', () => {
  const roomSrc = fs.readFileSync('frontend/src/pages/RoomDetailPage.jsx', 'utf8');

  // Preserved stay criteria strip
  assert.ok(
    roomSrc.includes('room-detail__stay-criteria'),
    'RoomDetailPage must render stay criteria container'
  );
  assert.ok(
    roomSrc.includes('formatDisplayDate(checkIn)'),
    'RoomDetailPage must display formatted check-in date'
  );
  assert.ok(
    roomSrc.includes('formatDisplayDate(checkOut)'),
    'RoomDetailPage must display formatted check-out date'
  );

  // Clear "Change dates" action
  assert.ok(
    roomSrc.includes('room-detail__change-dates-btn'),
    'RoomDetailPage must render .room-detail__change-dates-btn'
  );
  assert.ok(
    roomSrc.includes('Change dates'),
    'Action button text must be "Change dates"'
  );

  // Returns to backUrl (/stay with all query parameters intact)
  assert.ok(
    roomSrc.includes('const backUrl = `/stay?checkIn=${encodeURIComponent(checkIn)}&checkOut=${encodeURIComponent(checkOut)}&rooms=${rooms}&adults=${adults}&children=${children}`;'),
    'backUrl must preserve checkIn, checkOut, rooms, adults, children'
  );
  assert.ok(
    roomSrc.includes('<Link to={backUrl} className="room-detail__change-dates-btn"'),
    '"Change dates" button must link to backUrl'
  );
});

test('Issue 3: Single primary action per relevant page without header competition', () => {
  const layoutSrc = fs.readFileSync('frontend/src/components/layout/GuestLayout.jsx', 'utf8');

  // Header conditionally renders CTA based on route
  assert.ok(
    layoutSrc.includes('const showHeaderCta = shouldShowHeaderCta(location.pathname);'),
    'GuestLayout must check shouldShowHeaderCta with location.pathname'
  );
  assert.ok(
    layoutSrc.includes('{showHeaderCta && ('),
    'GuestLayout header CTA must be conditionally rendered'
  );

  // GuestAvailabilityPage has exactly one primary action: "Search availability"
  const availSrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.jsx', 'utf8');
  assert.ok(
    availSrc.includes('Search availability'),
    'GuestAvailabilityPage action button must be labeled "Search availability"'
  );
});
