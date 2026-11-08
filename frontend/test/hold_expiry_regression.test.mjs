import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { parseServerExpiry } from '../src/utils/dateUtils.js';

test('Issue 1 Regression: parseServerExpiry parses UTC ISO string ending with Z accurately', () => {
  const utcIso = '2026-11-08T04:15:30Z';
  const expectedEpoch = Date.parse(utcIso);
  const result = parseServerExpiry(utcIso);

  assert.equal(typeof result, 'number');
  assert.equal(result, expectedEpoch, 'Must match exact UTC epoch milliseconds');
  assert.equal(result, 1794111330000);
});

test('Issue 1 Regression: parseServerExpiry prevents Asia/Kolkata timezone shift on timezone-less timestamp', () => {
  // Legacy or timezone-less server timestamp
  const rawTimestamp = '2026-11-08T04:15:30';
  const withZ = '2026-11-08T04:15:30Z';
  const expectedUtcEpoch = Date.parse(withZ);

  const result = parseServerExpiry(rawTimestamp);

  assert.equal(
    result,
    expectedUtcEpoch,
    'Timezone-less timestamp must be parsed strictly as UTC, matching the Z epoch timestamp'
  );
  assert.equal(result, 1794111330000);
});

test('Issue 1 Regression: parseServerExpiry correctly parses offset-bearing ISO strings', () => {
  // 04:15:30 UTC is 09:45:30 IST (+05:30)
  const istIso = '2026-11-08T09:45:30+05:30';
  const result = parseServerExpiry(istIso);

  assert.equal(result, 1794111330000, 'Must normalize offset timestamp to exact UTC instant');
});

test('Issue 1 Regression: parseServerExpiry handles malformed, empty, and null values safely', () => {
  assert.equal(parseServerExpiry(null), null, 'null must return null');
  assert.equal(parseServerExpiry(undefined), null, 'undefined must return null');
  assert.equal(parseServerExpiry(''), null, 'empty string must return null');
  assert.equal(parseServerExpiry('   '), null, 'whitespace string must return null');
  assert.equal(parseServerExpiry('not-a-date'), null, 'invalid text must return null');
  assert.equal(parseServerExpiry('{}'), null, 'JSON string must return null');
  assert.equal(parseServerExpiry(true), null, 'boolean must return null');
});

test('Issue 1 Regression: parseServerExpiry handles numeric epoch timestamps', () => {
  const epochMs = 1794111330000;
  assert.equal(parseServerExpiry(epochMs), epochMs, 'epoch ms must return unchanged');
  assert.equal(parseServerExpiry(epochMs / 1000), epochMs, 'epoch seconds must convert to ms');
  assert.equal(parseServerExpiry(0), null, 'zero must return null');
  assert.equal(parseServerExpiry(-100), null, 'negative must return null');
});

test('Issue 1 Regression: Fresh hold expiry yields positive countdown and never falsely expires', () => {
  // Simulate server creating a hold 10 minutes (600s) from now in UTC
  const nowMs = Date.now();
  const expiresAtUtcIso = new Date(nowMs + 600 * 1000).toISOString();

  const parsedTarget = parseServerExpiry(expiresAtUtcIso);
  assert.ok(parsedTarget, 'Target time must parse successfully');

  const diffSeconds = Math.floor((parsedTarget - nowMs) / 1000);
  assert.ok(
    diffSeconds >= 598 && diffSeconds <= 600,
    `Remaining hold countdown must be ~600 seconds, got ${diffSeconds}s (not negative or zero)`
  );
});

test('Source Contract: GuestPaymentPage does not independently remove token or set expired on client countdown tick', () => {
  const paymentSrc = fs.readFileSync('frontend/src/pages/GuestPaymentPage.jsx', 'utf8');

  // Verify parseServerExpiry is imported and used
  assert.ok(
    paymentSrc.includes('import { formatDisplayDate, parseServerExpiry } from \'../utils/dateUtils\';'),
    'GuestPaymentPage must import parseServerExpiry from dateUtils'
  );

  // Find tick function in the countdown effect
  const tickIdx = paymentSrc.indexOf('function tick()');
  assert.ok(tickIdx !== -1, 'tick function must be defined');
  const tickBody = paymentSrc.substring(tickIdx, tickIdx + 300);

  // Ensure tick does NOT call setIsExpired(true) or removeItem
  assert.ok(
    !tickBody.includes('setIsExpired(true)'),
    'Countdown tick must NOT autonomously set isExpired(true)'
  );
  assert.ok(
    !tickBody.includes('sessionStorage.removeItem'),
    'Countdown tick must NOT autonomously delete payment token from sessionStorage'
  );

  // Ensure handleSimulate catch checks server-side expiration authoritative signals
  assert.ok(
    paymentSrc.includes('err.status === 409') && paymentSrc.includes("includes('expired')"),
    'handleSimulate must respond to authoritative server-side 409 expired rejection'
  );
});
