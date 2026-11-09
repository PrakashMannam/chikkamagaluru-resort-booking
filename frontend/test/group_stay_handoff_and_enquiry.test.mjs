import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('Group Handoff Contract: /stay routes 6+ rooms to /enquire?type=room&group=1 without numeric 6 in checkout', () => {
  const staySrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.jsx', 'utf8');

  // Verify group handoff routes to Estate Stays (?type=room&group=1) and never weddings/offsites
  assert.ok(
    staySrc.includes('/enquire?type=room&group=1'),
    'Group handoff must route to /enquire?type=room&group=1'
  );
  assert.ok(
    !staySrc.includes('/enquire?type=wedding&group=1') && !staySrc.includes('/enquire?type=offsite&group=1'),
    'Group handoff must never route to weddings or offsites'
  );

  // Verify 6+ is not passed as a numeric room parameter to availability search or checkout
  assert.ok(
    staySrc.includes("if (formRooms === 'group' || Number(formRooms) >= 6)"),
    'Group selections must be intercepted before search execution'
  );
  assert.ok(
    staySrc.includes('isLargeGroupStay ?'),
    'Large group stays must replace direct room checkout with group proposal handoff'
  );

  // Verify dates and guest counts are preserved across the handoff
  assert.ok(
    staySrc.includes('checkIn=${encodeURIComponent(checkIn)}'),
    'Handoff must preserve checkIn date when present'
  );
  assert.ok(
    staySrc.includes('guests=${adults + children}'),
    'Handoff must preserve guest counts'
  );

  // Verify group callout copy on /stay
  assert.ok(
    staySrc.includes('Planning a Multi-Room Group Stay?'),
    'Must display exact group heading'
  );
  assert.ok(
    staySrc.includes('Our reservations team will review your dates, group size, and room preferences. Sending this request does not reserve or hold rooms.'),
    'Must display exact group notice stating rooms are not reserved or held'
  );
  assert.ok(
    staySrc.includes('Request a group-stay proposal &rarr;'),
    'Must display exact group CTA text'
  );
});

test('Estate Stays Group Enquiry View: Header, field prefilling, hiding rules, and disclaimer contract', () => {
  const enquirySrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Group mode header
  assert.ok(
    enquirySrc.includes("title: 'Request a Group Stay Proposal'"),
    'Must display "Request a Group Stay Proposal" title in group mode'
  );
  assert.ok(
    enquirySrc.includes("kicker: 'Mullayanagiri Foothills, Chikkamagaluru'"),
    'Must display regional eyebrow in group mode'
  );
  assert.ok(
    enquirySrc.includes("For family getaways, multi-room bookings, and extended plantation retreats, our reservations team will review your request."),
    'Must display exact group subtitle'
  );

  // Group mode hides occasion selector and direct booking banner
  assert.ok(
    enquirySrc.includes('!isGroupMode && (') && enquirySrc.includes('Choose Your Occasion'),
    'Must hide the 3-occasion selector in group mode'
  );
  assert.ok(
    enquirySrc.includes('!isGroupMode && isRoomBooking && ('),
    'Must hide the immediate room booking banner in group mode'
  );

  // Normal direct enquiry visit preserves occasion selector and banner
  assert.ok(
    enquirySrc.includes('Choose Your Occasion'),
    'Occasion chooser must remain available for normal direct visits'
  );
  assert.ok(
    enquirySrc.includes('Looking for Immediate Room Bookings?'),
    'Immediate booking banner must remain available for normal direct room visits'
  );

  // Group mode submit button text and disclaimer
  assert.ok(
    enquirySrc.includes("isGroupMode ? 'Send group-stay request' : 'Submit Enquiry'"),
    'Submit button must say "Send group-stay request" in group mode and "Submit Enquiry" normally'
  );
  assert.ok(
    enquirySrc.includes('Sending this enquiry does not reserve or hold rooms.'),
    'Disclaimer must explicitly state that sending enquiry does not reserve or hold rooms'
  );

  // Weddings and Offsites remain intact
  assert.ok(enquirySrc.includes("Weddings & Celebrations Enquiry"));
  assert.ok(enquirySrc.includes("Corporate Offsites & Leadership Retreats"));
});

test('Responsive Layout: CSS defines group card styling and mobile full-width CTA', () => {
  const cssSrc = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.css', 'utf8');

  assert.ok(cssSrc.includes('.enquiry-card--group'), 'Must define .enquiry-card--group styling');
  assert.ok(cssSrc.includes('.enquiry-back-link'), 'Must style subtle back link');
  assert.ok(cssSrc.includes('@media (max-width: 639px)'), 'Must include mobile responsive styles');
  assert.ok(cssSrc.includes('width: 100%'), 'Must ensure full-width CTA on mobile');
});
