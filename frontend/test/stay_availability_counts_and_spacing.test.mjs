import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('Stay Availability Counts: GuestAvailabilityPage computes and renders per-category remaining allotment', () => {
  const staySrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.jsx', 'utf8');

  // Verify dynamic remainingAllotment usage
  assert.ok(
    staySrc.includes('const availableCount = cat.remainingAllotment ?? cat.availableRooms ?? 0;'),
    'Must compute availableCount using remainingAllotment from API response'
  );

  // Verify per-category count display in title row and badge
  assert.ok(
    staySrc.includes('${availableCount} available'),
    'Must display dynamic "{availableCount} available" label'
  );
  assert.ok(
    staySrc.includes('stay-avail-pill'),
    'Must render availability pill alongside category title'
  );
  assert.ok(
    staySrc.includes('Sold out'),
    'Must explicitly display "Sold out" when allotment is zero'
  );
  assert.ok(
    staySrc.includes('No availability'),
    'Must explicitly display "No availability" when category is blacked out'
  );
});

test('Stay Spacing: GuestAvailabilityPage.css establishes compact vertical spacing and balanced 3-part card layout', () => {
  const cssSrc = fs.readFileSync('frontend/src/pages/GuestAvailabilityPage.css', 'utf8');

  // Check 3-part layout
  assert.ok(
    cssSrc.includes('grid-template-columns: 320px 1fr 240px;') || cssSrc.includes('grid-template-columns: 360px 1fr 240px;'),
    'Must establish balanced 3-part grid layout for room cards'
  );
  assert.ok(
    cssSrc.includes('.stay-room-card__title-row'),
    'Must style title row with space-between alignment for title and availability pill'
  );
  assert.ok(
    cssSrc.includes('.stay-avail-pill'),
    'Must style availability pills'
  );

  // Check discovery section responsive grid (2-column on desktop)
  assert.ok(
    cssSrc.includes('.stay-discovery-grid'),
    'Must define stay discovery grid'
  );
  assert.ok(
    cssSrc.includes('grid-template-columns: repeat(2, 1fr);'),
    'Discovery cards must be arranged in 2 balanced columns on desktop'
  );
});
