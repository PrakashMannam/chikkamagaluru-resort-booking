import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('Stay Search Capacity: GuestAvailabilityPage provides 1–5 room options, 6+ group option, and dynamic guest ceilings', () => {
  const staySrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.jsx', 'utf8');

  // Verify room options up to 5 rooms and 6+ group stay
  assert.ok(
    staySrc.includes('<option value={5}>5 Rooms</option>'),
    'Must include option for 5 Rooms'
  );
  assert.ok(
    staySrc.includes('6+ Rooms (Group Stay)'),
    'Must include option for 6+ Rooms (Group Stay)'
  );

  // Verify dynamic adult and child ceiling calculations
  assert.ok(
    staySrc.includes('const maxAdults = isGroupSelected ? 30 : numericFormRooms * 3;'),
    'Must dynamically compute max adults up to 3 adults per key'
  );
  assert.ok(
    staySrc.includes('const maxChildren = isGroupSelected ? 12 : numericFormRooms * 2;'),
    'Must dynamically compute max children up to 2 children per key'
  );

  // Verify group concierge handoff copy
  assert.ok(
    staySrc.includes('Planning a Multi-Room Group Stay?'),
    'Must render group concierge title'
  );
  assert.ok(
    staySrc.includes('Sending this request does not reserve or hold rooms.'),
    'Must render group concierge description with disclaimer'
  );
  assert.ok(
    staySrc.includes('Request a group-stay proposal'),
    'Must provide Request a group-stay proposal CTA button'
  );

  // Verify multi-room pricing label
  assert.ok(
    staySrc.includes('Rooms ×'),
    'Must format multi-room price totals as X Rooms × Y Nights'
  );
});

test('Stay Search Layout: Desktop search edit grid aligns all 5 inputs and action button in a single row', () => {
  const cssSrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.css', 'utf8');

  assert.ok(
    cssSrc.includes('@media (min-width: 1024px)') && cssSrc.includes('.stay-search-edit__grid'),
    'Must define desktop media query for search edit grid'
  );
  assert.ok(
    cssSrc.includes('.stay-group-banner'),
    'Must define styles for group concierge banner'
  );
  assert.ok(
    cssSrc.includes('.stay-capacity-warning'),
    'Must style capacity warning for multi-room searches exceeding limits'
  );
});
