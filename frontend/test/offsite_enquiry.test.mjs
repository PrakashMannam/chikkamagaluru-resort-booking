import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  buildRawEnquiryMessage,
  DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION
} from '../src/utils/enquiryMessageBuilder.js';

test('Behavioral: DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION is neutral "unspecified"', () => {
  assert.equal(
    DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION,
    'unspecified',
    'Default conference hall selection constant must be "unspecified"'
  );
});

test('Behavioral: Corporate Offsite message omits conference hall when default (unspecified) or explicit "no"', () => {
  const baseParams = {
    enquiryType: 'CORPORATE_OFFSITE',
    organization: 'Acme Corp',
    eventDate: '2026-11-20',
    nights: '2',
    guestsCount: '45',
    roomsCount: '20',
  };

  // 1. Default unspecified
  const defaultMsg = buildRawEnquiryMessage({
    ...baseParams,
    needsConferenceHall: 'unspecified',
  });
  assert.ok(
    !defaultMsg.includes('with a conference hall'),
    'Default unspecified must not claim or append "with a conference hall"'
  );
  assert.ok(
    !defaultMsg.toLowerCase().includes('av'),
    'Default unspecified must not mention AV setup'
  );

  // 2. Explicit 'no'
  const noMsg = buildRawEnquiryMessage({
    ...baseParams,
    needsConferenceHall: 'no',
  });
  assert.ok(
    !noMsg.includes('with a conference hall'),
    'Explicit "no" must not claim or append "with a conference hall"'
  );

  // 3. Explicit 'yes'
  const yesMsg = buildRawEnquiryMessage({
    ...baseParams,
    needsConferenceHall: 'yes',
  });
  assert.ok(
    yesMsg.includes('with a conference hall'),
    'Explicit "yes" must include "with a conference hall" in the message'
  );
  assert.equal(
    yesMsg,
    'Corporate offsite for Acme Corp, Team of 45, wants an offsite on 2026-11-20, for 2 nights, with 20 rooms, with a conference hall'
  );
});

test('Source contract: PublicEnquiryPage initializes and resets conference hall to unspecified with neutral option', () => {
  const fileContent = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // Initial state uses DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION
  assert.match(
    fileContent,
    /needsConferenceHall,\s*setNeedsConferenceHall\]\s*=\s*useState\(DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION\)/,
    'PublicEnquiryPage must initialize needsConferenceHall to DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION'
  );

  // Switching to CORPORATE_OFFSITE resets to DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION
  assert.match(
    fileContent,
    /setNeedsConferenceHall\(DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION\)/,
    'handleTypeChange must reset needsConferenceHall to DEFAULT_OFFSITE_CONFERENCE_HALL_SELECTION'
  );

  // Dropdown includes neutral option
  const selectBlock = fileContent.match(/<select[^>]*id="conf-hall"[\s\S]*?<\/select>/);
  assert.ok(selectBlock, 'Meeting space select element must exist');
  assert.match(
    selectBlock[0],
    /<option value="unspecified">Not specified \/ please advise<\/option>/,
    'Select element must include neutral "unspecified" option'
  );
});

test('Privacy page accurately reflects confirmation link access without claiming public lookup', () => {
  const privacyContent = fs.readFileSync('frontend/src/pages/PrivacyPage.jsx', 'utf8');

  // Must NOT claim unauthenticated lookup by booking reference
  assert.doesNotMatch(
    privacyContent,
    /Allowing guests to look up their reservation details using a unique booking reference/,
    'Privacy page must not claim unauthenticated reservation lookup exists'
  );

  // Must describe direct confirmation & receipt access via confirmation link
  assert.match(
    privacyContent,
    /Allowing guests to access their reservation confirmation and receipt directly via the booking's confirmation link/,
    'Privacy page must describe access via confirmation link'
  );
  assert.match(
    privacyContent,
    /No unauthenticated public lookup directory is exposed/,
    'Privacy page must state no unauthenticated public lookup directory is exposed'
  );
});
