import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('Offsite meeting-space default is not Yes and only explicit Yes is included in payload', () => {
  const fileContent = fs.readFileSync('frontend/src/pages/PublicEnquiryPage.jsx', 'utf8');

  // 1. Initial state is 'unspecified' and NOT 'yes'
  assert.doesNotMatch(
    fileContent,
    /needsConferenceHall,\s*setNeedsConferenceHall\]\s*=\s*useState\(['"]yes['"]\)/,
    'Initial needsConferenceHall state must NOT be "yes"'
  );
  assert.match(
    fileContent,
    /needsConferenceHall,\s*setNeedsConferenceHall\]\s*=\s*useState\(['"]unspecified['"]\)/,
    'Initial needsConferenceHall state must be "unspecified"'
  );

  // 2. handleTypeChange resets to 'unspecified' on switching to CORPORATE_OFFSITE
  assert.match(
    fileContent,
    /setNeedsConferenceHall\(['"]unspecified['"]\)/,
    'Switching to CORPORATE_OFFSITE must set needsConferenceHall to "unspecified"'
  );

  // 3. Select dropdown offers neutral 'unspecified' default option
  const selectBlock = fileContent.match(/<select[^>]*id="conf-hall"[\s\S]*?<\/select>/);
  assert.ok(selectBlock, 'Meeting space select element must exist');
  assert.match(
    selectBlock[0],
    /<option value="unspecified">Not specified \/ please advise<\/option>/,
    'Select element must include neutral "unspecified" option'
  );

  // 4. Raw message builder only appends conference space if explicitly 'yes'
  assert.match(
    fileContent,
    /needsConferenceHall === ['"]yes['"] \? ['"]with a conference hall['"] : null/,
    'Payload must only append "with a conference hall" when explicitly chosen as "yes"'
  );
});

test('Privacy page accurately reflects confirmation link access without claiming public lookup', () => {
  const privacyContent = fs.readFileSync('frontend/src/pages/PrivacyPage.jsx', 'utf8');

  // 1. Must NOT claim guests can look up reservations using a booking reference
  assert.doesNotMatch(
    privacyContent,
    /Allowing guests to look up their reservation details using a unique booking reference/,
    'Privacy page must not claim unauthenticated reservation lookup exists'
  );

  // 2. Must describe direct confirmation & receipt access via confirmation link
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
